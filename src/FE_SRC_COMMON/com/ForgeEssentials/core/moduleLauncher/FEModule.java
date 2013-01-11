package com.ForgeEssentials.core.moduleLauncher;

import net.minecraft.command.ICommandSender;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public @interface FEModule
{
	/**
	 * this may be null
	 */
	Class<? extends IModuleConfig> configClass() default DummyConfig.class;
	
	/**
	 * "Module" is not automatically
	 * ie: "WorldControlModule" "SnooperModule" etc..
	 * this is what will show up in logs, especially about errors.
	 * This is similair to the ModuleID, it is the identifying mark.. and shouldn't have spaces.
	 */
	String name();
	
	String version() default "";
	
	/**
	 * Marks it as core. Core modules are loaded first. 
	 * @return
	 */
	boolean isCore() default false;
	
	/**
	 * For all built in modules, this had better be the ForgeEssentials class.
	 * For everyone else, this should be your @mod file.
	 * @return
	 */
	Class parentMod();
	
	/**
	 * Configs are instantiated before this.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD })
	public @interface PreInit {}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD })
	public @interface Init {}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD })
	public @interface PostInit {}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD })
	public @interface ServerInit {}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD })
	public @interface ServerPostInit {}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD })
	public @interface ServerStop {}
	
	/**
	 * this should be obvious, This is the method that will be called when the /reload command is called.
	 * Configs are relaoded just before this method is called.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD })
	public @interface Reload {}
	
	/**
	 * This field will be populated with an instance of this Module.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD })
	public @interface instance {}
	
	/**
	 * This field will be populated with an instance of this Module's parent mod.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD })
	public @interface ParentMod {}
	
	/**
	 * This field will be populated with an instance of this Module's ModuleContainer object.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD })
	public @interface Container {}
	
	/**
	 * This field will be populated with an instance of this Module's Config object.
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD })
	public @interface Config {}
	
	class DummyConfig implements IModuleConfig
	{

		@Override
		public void setGenerate(boolean generate)
		{
			
		}

		@Override
		public void init()
		{
		}

		@Override
		public void forceSave()
		{
		}

		@Override
		public void forceLoad(ICommandSender sender)
		{
		}

		@Override
		public File getFile()
		{
			return null;
		}
		
	}
}
