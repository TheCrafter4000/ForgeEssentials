package com.forgeessentials.commands.item;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityDropper;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameData;
import net.minecraftforge.permission.PermissionLevel;

import com.forgeessentials.api.UserIdent;
import com.forgeessentials.commands.util.FEcmdModuleCommands;
import com.forgeessentials.core.misc.TranslatedCommandException;
import com.forgeessentials.util.output.ChatOutputHandler;

public class CommandDrop extends FEcmdModuleCommands
{

    @Override
    public String getCommandName()
    {
        return "drop";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender par1ICommandSender)
    {
        return "/drop <X> <Y> <Z> <ItemID> <Meta> <Qty>";
    }

    @SuppressWarnings("deprecation")
    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length != 6)
        {
            throw new TranslatedCommandException(getCommandUsage(sender));
        }
        World world = null;
        int x = (int) this.func_82368_a(sender, 0.0D, args[0]);
        int y = (int) this.func_82367_a(sender, 0.0D, args[1], 0, 0);
        int z = (int) this.func_82368_a(sender, 0.0D, args[2]);

        if (sender instanceof DedicatedServer)
        {
            world = ((DedicatedServer) sender).worldServerForDimension(0);
        }
        else if (sender instanceof EntityPlayerMP)
        {
            world = ((Entity) sender).worldObj;
            x = (int) this.func_82368_a(sender, ((Entity) sender).posX, args[0]);
            y = (int) this.func_82367_a(sender, ((Entity) sender).posY, args[1], 0, 0);
            z = (int) this.func_82368_a(sender, ((Entity) sender).posZ, args[2]);
        }
        else if (sender instanceof TileEntity)
        {
            world = ((TileEntity) sender).getWorld();
            x = (int) this.func_82368_a(sender, ((TileEntity) sender).getPos().getX(), args[0]);
            y = (int) this.func_82367_a(sender, ((TileEntity) sender).getPos().getY(), args[1], 0, 0);
            z = (int) this.func_82368_a(sender, ((TileEntity) sender).getPos().getZ(), args[2]);
        }
        BlockPos pos = new BlockPos(x, y, z);
        
        String var7 = args[3];
        int var8 = parseInt(args[4], 0, Integer.MAX_VALUE);
        int var9 = parseInt(args[5], 1, GameData.getItemRegistry().getObject(var7).getItemStackLimit());
        ItemStack tmpStack;

        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof TileEntityChest)
        {
            TileEntityChest var10 = (TileEntityChest) tileEntity;

            for (int slot = 0; slot < var10.getSizeInventory(); ++slot)
            {
                if (var10.getStackInSlot(slot) == null)
                {
                    var10.setInventorySlotContents(slot, new ItemStack(GameData.getItemRegistry().getObject(var7), var9, var8));
                    break;
                }

                if (var10.getStackInSlot(slot).getUnlocalizedName() == var7 && var10.getStackInSlot(slot).getItemDamage() == var8)
                {
                    if (var10.getStackInSlot(slot).getMaxStackSize() - var10.getStackInSlot(slot).stackSize >= var9)
                    {
                        tmpStack = var10.getStackInSlot(slot);
                        tmpStack.stackSize += var9;
                        break;
                    }

                    var9 -= var10.getStackInSlot(slot).getMaxStackSize() - var10.getStackInSlot(slot).stackSize;
                    var10.getStackInSlot(slot).stackSize = var10.getStackInSlot(slot).getMaxStackSize();
                }
            }
        }
        else if (tileEntity instanceof TileEntityDropper)
        {
            TileEntityDropper var13 = (TileEntityDropper) tileEntity;

            for (int slot = 0; slot < var13.getSizeInventory(); ++slot)
            {
                if (var13.getStackInSlot(slot) == null)
                {
                    var13.setInventorySlotContents(slot, new ItemStack(GameData.getItemRegistry().getObject(var7), var9, var8));
                    break;
                }

                if (var13.getStackInSlot(slot).getUnlocalizedName() == var7 && var13.getStackInSlot(slot).getItemDamage() == var8)
                {
                    if (var13.getStackInSlot(slot).getMaxStackSize() - var13.getStackInSlot(slot).stackSize >= var9)
                    {
                        tmpStack = var13.getStackInSlot(slot);
                        tmpStack.stackSize += var9;
                        break;
                    }

                    var9 -= var13.getStackInSlot(slot).getMaxStackSize() - var13.getStackInSlot(slot).stackSize;
                    var13.getStackInSlot(slot).stackSize = var13.getStackInSlot(slot).getMaxStackSize();
                }
            }
        }
        else if (tileEntity instanceof TileEntityDispenser)
        {
            TileEntityDispenser var14 = (TileEntityDispenser) tileEntity;

            for (int slot = 0; slot < var14.getSizeInventory(); ++slot)
            {
                if (var14.getStackInSlot(slot) == null)
                {
                    var14.setInventorySlotContents(slot, new ItemStack(GameData.getItemRegistry().getObject(var7), var9, var8));
                    break;
                }

                if (var14.getStackInSlot(slot).getUnlocalizedName() == var7 && var14.getStackInSlot(slot).getItemDamage() == var8)
                {
                    if (var14.getStackInSlot(slot).getMaxStackSize() - var14.getStackInSlot(slot).stackSize >= var9)
                    {
                        tmpStack = var14.getStackInSlot(slot);
                        tmpStack.stackSize += var9;
                        break;
                    }

                    var9 -= var14.getStackInSlot(slot).getMaxStackSize() - var14.getStackInSlot(slot).stackSize;
                    var14.getStackInSlot(slot).stackSize = var14.getStackInSlot(slot).getMaxStackSize();
                }
            }
        }
        else if (tileEntity instanceof TileEntityHopper)
        {
            TileEntityHopper var12 = (TileEntityHopper) tileEntity;

            for (int slot = 0; slot < var12.getSizeInventory(); ++slot)
            {
                if (var12.getStackInSlot(slot) == null)
                {
                    var12.setInventorySlotContents(slot, new ItemStack(GameData.getItemRegistry().getObject(var7), var9, var8));
                    var9 = 0;
                    break;
                }

                if (var12.getStackInSlot(slot).getUnlocalizedName() == var7 && var12.getStackInSlot(slot).getItemDamage() == var8)
                {
                    if (var12.getStackInSlot(slot).getMaxStackSize() - var12.getStackInSlot(slot).stackSize >= var9)
                    {
                        tmpStack = var12.getStackInSlot(slot);
                        tmpStack.stackSize += var9;
                        var9 = 0;
                        break;
                    }

                    var9 -= var12.getStackInSlot(slot).getMaxStackSize() - var12.getStackInSlot(slot).stackSize;
                    var12.getStackInSlot(slot).stackSize = var12.getStackInSlot(slot).getMaxStackSize();
                }
            }
        }
        else
        {
            throw new TranslatedCommandException("No viable container found to put item in.");
        }
        if (var9 > 0)
        {
            throw new TranslatedCommandException("Not enough room for items.");
        }
        ChatOutputHandler.chatConfirmation(sender, "Items dropped into container.");
    }

    private double func_82368_a(ICommandSender par1ICommandSender, double par2, String par4Str) throws CommandException
    {
        return this.func_82367_a(par1ICommandSender, par2, par4Str, -30000000, 30000000);
    }

    private double func_82367_a(ICommandSender par1ICommandSender, double par2, String par4Str, int par5, int par6) throws CommandException
    {
        boolean flag = par4Str.startsWith("~");
        double d1 = flag ? par2 : 0.0D;

        if (!flag || par4Str.length() > 1)
        {
            boolean flag1 = par4Str.contains(".");

            if (flag)
            {
                par4Str = par4Str.substring(1);
            }

            d1 += parseDouble(par4Str);

            if (!flag1 && !flag)
            {
                d1 += 0.5D;
            }
        }

        if (par5 != 0 || par6 != 0)
        {
            if (d1 < par5)
            {
                throw new NumberInvalidException("commands.generic.double.tooSmall", new Object[] { Double.valueOf(d1), Integer.valueOf(par5) });
            }

            if (d1 > par6)
            {
                throw new NumberInvalidException("commands.generic.double.tooBig", new Object[] { Double.valueOf(d1), Integer.valueOf(par6) });
            }
        }

        return d1;
    }

    @Override
    public PermissionLevel getPermissionLevel()
    {
        return PermissionLevel.OP;
    }

    @Override
    public void processCommandPlayer(EntityPlayerMP sender, String[] args) throws CommandException
    {
        EntityPlayerMP playermp = UserIdent.getPlayerByMatchOrUsername(sender, sender.getName());
        processCommand(playermp, args);
    }

    @Override
    public void processCommandConsole(ICommandSender sender, String[] args) throws CommandException
    {
        processCommand(sender, args);
    }

    @Override
    public boolean canConsoleUseCommand()
    {
        return true;
    }
}