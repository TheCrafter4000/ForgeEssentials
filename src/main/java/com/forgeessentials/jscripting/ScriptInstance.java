package com.forgeessentials.jscripting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.stream.Collectors;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import net.minecraft.command.ICommandSender;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.permission.PermissionLevel;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.forgeessentials.core.commands.ParserCommandBase;
import com.forgeessentials.core.misc.FECommandManager;
import com.forgeessentials.core.misc.TaskRegistry;
import com.forgeessentials.core.misc.TaskRegistry.RunLaterTimerTask;
import com.forgeessentials.jscripting.command.CommandJScriptCommand;
import com.forgeessentials.jscripting.wrapper.JsWindowStatic;
import com.forgeessentials.jscripting.wrapper.event.JsEvent;
import com.forgeessentials.jscripting.wrapper.item.JsItemStatic;
import com.forgeessentials.jscripting.wrapper.server.JsPermissionsStatic;
import com.forgeessentials.jscripting.wrapper.server.JsServerStatic;
import com.forgeessentials.jscripting.wrapper.world.JsBlockStatic;
import com.forgeessentials.jscripting.wrapper.world.JsWorldStatic;
import com.forgeessentials.util.output.ChatOutputHandler;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ScriptInstance
{

    public static final String SCRIPT_ERROR_TEXT = "Script error: ";

    public static final String WRAPPER_PACKAGE = "com.forgeessentials.jscripting.wrapper";

    private static String INIT_SCRIPT;

    public static class ProptertiesInfo<T>
    {

        public final Class<T> clazz;

        public final List<Field> fields = new ArrayList<>();

        public final CompiledScript script;

        public ProptertiesInfo(Class<T> clazz) throws ScriptException
        {
            this.clazz = clazz;
            for (Field field : clazz.getDeclaredFields())
            {
                if (field.isAccessible())
                {
                    fields.add(field);
                }
            }
            String scriptSrc = fields.stream().map((f) -> "o." + f.getName()).collect(Collectors.joining(",", "[", "]"));
            script = propertyEngine.compile(scriptSrc);
        }

        public ProptertiesInfo(Class<T> clazz, T instance) throws ScriptException
        {
            this.clazz = clazz;
            for (Field field : clazz.getDeclaredFields())
            {
                if ((field.getModifiers() & Modifier.PUBLIC) != 0)
                {
                    try
                    {
                        field.setAccessible(true);
                        field.set(instance, null);
                        fields.add(field);
                    }
                    catch (IllegalArgumentException | IllegalAccessException e)
                    {
                        // field will be ignored!
                        System.out.println("Ignoring deserialization field " + field.getName());
                    }
                }
            }
            String scriptSrc = fields.stream().map((f) -> "o." + f.getName()).collect(Collectors.joining(",", "[", "]"));
            script = propertyEngine.compile(scriptSrc);
        }
    }

    private static ScriptInstance lastActive;

    @SuppressWarnings("unused")
    private static CompiledScript initScript;

    private static Compilable propertyEngine = ModuleJScripting.getCompilable();

    private static Map<String, CompiledScript> propertyScripts = new HashMap<>();

    private static Map<Class<?>, ProptertiesInfo<?>> propertyInfos = new HashMap<>();

    @SuppressWarnings("rawtypes")
    private static Map<String, Class<? extends JsEvent>> eventTypes = new HashMap<>();

    private static SimpleBindings rootPkg = new SimpleBindings();

    public static PermissionLevelObj permissionLevelObj = new PermissionLevelObj();

    private File file;

    private long lastModified;

    private CompiledScript script;

    private Invocable invocable;

    @SuppressWarnings("unused")
    private Bindings exports;

    private SimpleBindings getPropertyBindings = new SimpleBindings();

    private Set<String> illegalFunctions = new HashSet<>();

    private Map<Integer, TimerTask> tasks = new HashMap<>();

    private List<CommandJScriptCommand> commands = new ArrayList<>();

    private Map<Object, JsEvent<?>> eventHandlers = new HashMap<>();

    private WeakReference<ICommandSender> lastSender;

    static
    {
        try
        {
            INIT_SCRIPT = IOUtils.toString(ScriptInstance.class.getResource("init.js"));
            initScript = ModuleJScripting.getCompilable().compile(INIT_SCRIPT);
        }
        catch (IOException | ScriptException e)
        {
            Throwables.propagate(e);
        }
        try
        {
            ImmutableSet<ClassInfo> classes = ClassPath.from(ScriptInstance.class.getClassLoader()).getTopLevelClassesRecursive(WRAPPER_PACKAGE);
            for (ClassInfo classInfo : classes)
            {
                registerWrapperClass(classInfo);
            }
        }
        catch (IOException e)
        {
            Throwables.propagate(e);
        }
    }

    public static Object toNashornClass(Class<?> c)
    {
        try
        {
            Class<?> cl = Class.forName("jdk.internal.dynalink.beans.StaticClass", true, ClassLoader.getSystemClassLoader());
            Constructor<?> constructor = cl.getDeclaredConstructor(Class.class);
            constructor.setAccessible(true);
            return constructor.newInstance(c);
        }
        catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e)
        {
            throw Throwables.propagate(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void registerWrapperClass(ClassInfo classInfo)
    {
        if (!classInfo.getSimpleName().startsWith("Js"))
            return;
        Class<?> clazz = classInfo.load();
        // if (!JsWrapper.class.isAssignableFrom(clazz))
        // return;

        String jsName = classInfo.getName().substring(WRAPPER_PACKAGE.length() + 1);
        String[] jsNameParts = jsName.split("\\.");
        SimpleBindings pkg = rootPkg;
        for (int i = 0; i < jsNameParts.length - 1; i++)
        {
            String name = StringUtils.capitalize(jsNameParts[i]);
            SimpleBindings parentPkg = pkg;
            pkg = (SimpleBindings) parentPkg.get(name);
            if (pkg == null)
            {
                pkg = new SimpleBindings();
                parentPkg.put(name, pkg);
            }
        }
        pkg.put(jsNameParts[jsNameParts.length - 1].substring(2), toNashornClass(clazz));

        // Check for event handlers
        try
        {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods)
            {
                if (method.getName().equals("_handle") && method.getParameterCount() == 1 && Event.class.isAssignableFrom(method.getParameterTypes()[0]))
                {
                    SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
                    if (annotation != null)
                    {
                        eventTypes.put(clazz.getSimpleName().substring(2), (Class<? extends JsEvent<?>>) clazz);
                        break;
                    }
                }
            }
        }
        catch (SecurityException e)
        {
            throw Throwables.propagate(e);
        }
    }

    public ScriptInstance(File file) throws IOException, ScriptException
    {
        if (!file.exists())
            throw new IllegalArgumentException("file");

        this.file = file;
        compileScript();
    }

    public void dispose()
    {
        for (TimerTask task : tasks.values())
            TaskRegistry.remove(task);
        tasks.clear();

        for (ParserCommandBase command : commands)
            FECommandManager.deegisterCommand(command.getCommandName());
        commands.clear();

        for (JsEvent<?> eventHandler : eventHandlers.values())
            eventHandler._unregister();
        eventHandlers.clear();
    }

    protected void compileScript() throws IOException, FileNotFoundException, ScriptException
    {
        illegalFunctions.clear();
        script = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            // Load and compile script
            script = ModuleJScripting.getCompilable().compile(reader);

            // Initialization of module environment
            ScriptEngine engine = script.getEngine();
            invocable = (Invocable) engine;
            engine.put("window", new JsWindowStatic(this));
            engine.put("Server", new JsServerStatic(this));
            engine.put("Block", new JsBlockStatic());
            engine.put("Item", new JsItemStatic());
            engine.put("World", new JsWorldStatic());
            engine.put("Permissions", new JsPermissionsStatic());
            engine.put("PermissionLevel", new PermissionLevelObj());
            engine.put("MC", rootPkg);

            // INIT_SCRIPT = IOUtils.toString(ScriptInstance.class.getResource("init.js")); // TODO: DEV ONLY REALOD OF INIT SCRIPT
            engine.eval(INIT_SCRIPT);

            // Start script
            script.eval();
            exports = (Bindings) engine.get("exports");
        }
        lastModified = file.lastModified();
    }

    public void checkIfModified() throws IOException, FileNotFoundException, ScriptException
    {
        if (file.exists() && file.lastModified() != lastModified)
            compileScript();
    }

    /* ************************************************************ */
    /* Script invocation */

    public Object callGlobal(String fn, Object... args) throws NoSuchMethodException, ScriptException
    {
        try
        {
            setLastActive();
            return this.invocable.invokeFunction(fn, args);
        }
        catch (Exception e)
        {
            illegalFunctions.add(fn);
            throw e;
        }
        finally
        {
            clearLastActive();
        }
    }

    public Object tryCallGlobal(String fn, Object... args) throws ScriptException
    {
        try
        {
            setLastActive();
            return this.invocable.invokeFunction(fn, args);
        }
        catch (NoSuchMethodException e)
        {
            illegalFunctions.add(fn);
            return null;
        }
        finally
        {
            clearLastActive();
        }
    }

    public boolean hasGlobalCallFailed(String fnName)
    {
        return illegalFunctions.contains(fnName);
    }

    public Object call(Object fn, Object thiz, Object... args) throws NoSuchMethodException, ScriptException
    {
        try
        {
            setLastActive();
            return this.invocable.invokeMethod(fn, "call", ArrayUtils.add(args, 0, thiz));
        }
        finally
        {
            clearLastActive();
        }
    }

    public Object tryCall(Object fn, Object thiz, Object... args) throws ScriptException
    {
        try
        {
            setLastActive();
            return this.invocable.invokeMethod(fn, "call", ArrayUtils.add(args, 0, thiz));
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
        finally
        {
            clearLastActive();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(Object object, String property) throws ScriptException
    {
        property = "o." + property;
        getPropertyBindings.put("o", object);
        CompiledScript propertyScript = propertyScripts.get(property);
        if (propertyScript == null)
        {
            propertyScript = propertyEngine.compile(property);
            propertyScripts.put(property, propertyScript);
        }
        return (T) propertyScript.eval(getPropertyBindings);
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperties(T instance, Object object, Class<T> clazz) throws ScriptException
    {
        ProptertiesInfo<T> props = (ProptertiesInfo<T>) propertyInfos.get(clazz);
        if (props == null)
        {
            props = new ProptertiesInfo<>(clazz, instance);
            propertyInfos.put(clazz, props);
        }

        if (object instanceof Bindings)
        {
            Bindings bindings = (Bindings) object;
            try
            {
                for (Field f : props.fields)
                    f.set(instance, bindings.get(f.getName()));
            }
            catch (IllegalArgumentException | IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            getPropertyBindings.put("o", object);
            Object eval = props.script.eval(getPropertyBindings);
            if (!(eval instanceof Bindings))
                throw new ScriptException("Unable to access properties");
            Bindings bindings = (Bindings) eval;
            try
            {
                for (int i = 0; i < props.fields.size(); i++)
                    props.fields.get(i).set(instance, bindings.get(i));
            }
            catch (IllegalArgumentException | IllegalAccessException e)
            {
                e.printStackTrace();
            }
        }
        return instance;
    }

    private void setLastActive()
    {
        // lastActive = new WeakReference<ScriptInstance>(this);
        lastActive = this;
    }

    private void clearLastActive()
    {
        lastActive = null;
    }

    public ScriptInstance getLastActive()
    {
        return lastActive;
    }

    /**
     * This should be called every time a script is invoked by a user to send errors to the correct user
     */
    public void setLastSender(ICommandSender sender)
    {
        this.lastSender = new WeakReference<>(sender);
    }

    /* ************************************************************ */
    /* Timeout & Promise handling */

    private RunLaterTimerTask createCallbackTask(Object fn, Object... args)
    {
        return new RunLaterTimerTask(() -> {
            try
            {
                call(fn, fn, args);
            }
            catch (NoSuchMethodException | ScriptException e)
            {
                chatError("Error in script callback: " + e.getMessage());
            }
        });
    }

    private int registerTimeout(TimerTask task)
    {
        int id = (int) Math.round(Math.random() * Integer.MAX_VALUE);
        while (tasks.containsKey(id))
            id = (int) Math.round(Math.random() * Integer.MAX_VALUE);
        tasks.put(id, task);
        return id;
    }

    public int setTimeout(Object fn, long timeout, Object... args)
    {
        TimerTask task = createCallbackTask(fn, args);
        TaskRegistry.schedule(task, timeout);
        return registerTimeout(task);
    }

    public int setInterval(Object fn, long timeout, Object... args)
    {
        TimerTask task = createCallbackTask(fn, args);
        TaskRegistry.scheduleRepeated(task, timeout);
        return registerTimeout(task);
    }

    public void clearTimeout(int id)
    {
        TimerTask task = tasks.remove(id);
        if (task != null)
            TaskRegistry.remove(task);
    }

    public void clearInterval(int id)
    {
        clearTimeout(id);
    }

    /* ************************************************************ */
    /* Event handling */

    public void registerScriptCommand(CommandJScriptCommand command)
    {
        commands.add(command);
        FECommandManager.registerCommand(command, true);
    }

    @SuppressWarnings({ "rawtypes" })
    public void registerEventHandler(String event, Object handler)
    {
        Class<? extends JsEvent> eventType = eventTypes.get(event);
        if (eventType == null)
        {
            chatError(SCRIPT_ERROR_TEXT + "Invalid event type " + event);
            return;
        }
        try
        {
            // Constructor<? extends JsEvent> constructor = eventType.getConstructor(ScriptInstance.class, Object.class);
            // JsEvent<?> eventHandler = constructor.newInstance(this, handler);
            JsEvent<?> eventHandler = eventType.newInstance();
            eventHandler._script = this;
            eventHandler._handler = handler;
            eventHandler._eventType = event;

            // TODO: Handle reuse of one handler for multiple events!
            eventHandlers.put(handler, eventHandler);

            eventHandler._register();
        }
        catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException e)
        {
            e.printStackTrace();
            chatError(SCRIPT_ERROR_TEXT + e.getMessage());
        }
    }

    public void unregisterEventHandler(Object handler)
    {
        JsEvent<?> eventHandler = eventHandlers.remove(handler);
        if (eventHandler == null)
            return;
        eventHandler._unregister();
    }

    /* ************************************************************ */
    /* Other & Utility */

    public File getFile()
    {
        return file;
    }

    public String getName()
    {
        String fileName = file.getAbsolutePath().substring(ModuleJScripting.getModuleDir().getAbsolutePath().length() + 1).replace('\\', '/');
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    public List<CommandJScriptCommand> getCommands()
    {
        return commands;
    }

    public List<String> getEventHandlers()
    {
        return eventHandlers.values().stream().map(x -> x.getEventType()).collect(Collectors.toList());
    }

    /**
     * Tries to send an error message to the last player using this script.<br>
     * If no player can be determined, the message will be broadcasted.
     *
     * @param message
     */
    public void chatError(String message)
    {
        chatError(lastSender == null ? null : lastSender.get(), message);
    }

    public void chatError(ICommandSender sender, String message)
    {
        IChatComponent msg = ChatOutputHandler.error(message);
        if (sender == null)
            ChatOutputHandler.broadcast(msg); // TODO: Replace with broadcast to admins only
        else
            ChatOutputHandler.sendMessage(sender, msg);
    }

    public static class PermissionLevelObj
    {
        public PermissionLevel TRUE = PermissionLevel.TRUE;
        public PermissionLevel OP = PermissionLevel.OP;
        public PermissionLevel FALSE = PermissionLevel.FALSE;
    }
}