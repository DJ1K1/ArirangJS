package com.arirangJS.Script;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.ItemStack;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.arirangJS.Debug.Debug;
import com.arirangJS.File.FileSystem;
import com.arirangJS.Main.Main;
import com.arirangJS.Script.Classes._Action;
import com.arirangJS.Script.Classes._Biome;
import com.arirangJS.Script.Classes._Bukkit;
import com.arirangJS.Script.Classes._ChatColor;
import com.arirangJS.Script.Classes._Effect;
import com.arirangJS.Script.Classes._Event;
import com.arirangJS.Script.Classes._Inventory;
import com.arirangJS.Script.Classes._Player;
import com.arirangJS.Script.Classes._Request;


public class ScriptManager implements Listener {
	
	public static void callMethod(String functionName, Object... args) {
		for(String filename : Main.scripts) {
			Context context = Context.enter();
			Scriptable scope = context.initStandardObjects();
			
			try {
				ScriptableObject.defineClass(scope, _Bukkit.class);
				ScriptableObject.defineClass(scope, _Player.class);
				ScriptableObject.defineClass(scope, _Event.class);
				ScriptableObject.defineClass(scope, _Inventory.class);
				ScriptableObject.defineClass(scope, _Effect.class);
				ScriptableObject.defineClass(scope, _Request.class);
				ScriptableObject.putProperty(scope, "ChatColor", constantsToObj(_ChatColor.class));
				ScriptableObject.putProperty(scope, "Biome", constantsToObj(_Biome.class));
				ScriptableObject.putProperty(scope, "Action", constantsToObj(_Action.class));
				
				FileInputStream inStream = new FileInputStream(FileSystem.LOC_TEMP+filename);
				BufferedReader reader = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
				context.evaluateReader(scope, reader, filename, 0, null);
				Object object = scope.get(functionName, scope);
				
				if(object != null && object instanceof Function) {
					Function function = (Function) object;
					function.call(context, scope, scope, args);
				}
			} catch(RhinoException e) {
				Debug.danger(e.getMessage()+" ("+e.lineNumber()+", "+e.columnNumber()+")");
			} catch(IOException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
				Debug.danger("An error occured while compiling code.");
			} finally {
				Context.exit();
			}
		}
	}
	
	public static ScriptableObject constantsToObj(Class<?> clazz) {
		ScriptableObject obj = new NativeObject();
		for(Field field : clazz.getFields()) {
			try {
				obj.putConst(field.getName(), obj, field.get(null));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				Debug.danger("An error occured translate class to jsObject");
				e.printStackTrace();
			}
		}
		
		return obj;
	}
	
	public static String locToJSON(Location location) {
		double x = location.getX();
		double y = location.getY();
		double z = location.getZ();
		float yaw = location.getYaw();
		float pitch = location.getPitch();
		int blockX = location.getBlockX();
		int blockY = location.getBlockY();
		int blockZ = location.getBlockZ();
		
		String result = String.format("({x: %f, y: %f, z: %f, yaw: %f, pitch: %f, blockX: %d, blockY: %d, blockZ: %d})",
				x, y, z, yaw, pitch, blockX, blockY, blockZ);
		return result;
	}
	
	@SuppressWarnings("deprecation")
	public static String blockToJSON(Block block) {
		if(block == null) return "({})";
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();
		int id = block.getTypeId();
		int damage = block.getData();
		int biome = block.getBiome().ordinal();
		
		String result = String.format("({x: %d, y: %d, z: %d, id: %d, damage: %d, biome: %d})",
				x, y, z, id, damage, biome);
		return result;
	}
	
	@SuppressWarnings("deprecation")
	public static String itemToJSON(ItemStack item) {
		if(item == null) return "({})";
		int id = item.getTypeId();
		int amount = item.getAmount();
		int durability = item.getDurability();
		String displayName = "";
		List<String> loreList;
		if(item.getItemMeta() == null) {
			displayName = "";
			loreList = new ArrayList<String>();
		} else if(item.getItemMeta().getDisplayName() == null) {
			displayName = "";
			loreList = new ArrayList<String>();
		} else {
			displayName = item.getItemMeta().getDisplayName();
			loreList = item.getItemMeta().getLore();
		}
		
		String enchantment = "{}";
		
		if(item.getEnchantments().size() > 0) {
			StringBuilder builder = new StringBuilder();
			// [[id,amount],[id,amount] ...]
			builder.append("{");
			for(Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
				builder.append(entry.getKey().getName()+": "+entry.getValue()+", ");
			}
			builder.setLength(builder.length()-2);
			builder.append("}");
			enchantment = builder.toString();
		}
		
		String lore = "[]";
		if(loreList != null) {
			if(loreList.size() != 0) {
				lore = "[";
				for(String str : loreList) {
					lore += "\""+str+"\",";
				}
				lore = lore.substring(0, lore.length()-1);
				lore += "]";
			}
		}
		
		String result = String.format("({id: %d, amount: %d, durability: %d, displayName: \"%s\", lore: %s, enchantment: %s})", id, amount, durability, displayName, lore, enchantment);
		return result;
	}
	
	public static String arrayToStr(Object[] arr) {
		String result = "[";
		if(arr.length == 0) return "[]";
		
		for(Object obj : arr) {
			result += obj.toString()+", ";
		}
		result = result.substring(0, result.length()-2);
		result += "]";
		return result;
	}
	
	public static String blockStateToJSON(BlockState state) {
		String result = String.format("({})");
		return result;
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent e) {
		Main.joinMessage = e.getJoinMessage();
		callMethod("onPlayerJoin", e.getPlayer().getName());
		
		e.setJoinMessage(Main.joinMessage);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent e) {
		Main.quitMessage = e.getQuitMessage();
		
		callMethod("onPlayerQuit", e.getPlayer().getName());
		
		e.setQuitMessage(Main.quitMessage);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent e) {
		Main.isCancelled.put(e.getEventName(), e.isCancelled());
		
		callMethod("onPlayerMove", e.getPlayer().getName(), locToJSON(e.getFrom()), locToJSON(e.getTo()));
		
		e.setCancelled(Main.isCancelled.get(e.getEventName()));
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerChat(AsyncPlayerChatEvent e) {
		Main.isCancelled.put(e.getEventName(), e.isCancelled());
		Main.chatFormat = e.getFormat();
		
		callMethod("onPlayerChat", e.getPlayer().getName(), e.getMessage());
		
		e.setFormat(Main.chatFormat);
		e.setCancelled(Main.isCancelled.get(e.getEventName()));
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent e) {
		Main.isCancelled.put(e.getEventName(), e.isCancelled());
		
		callMethod("onBlockPlace", e.getPlayer().getName(), blockToJSON(e.getBlock()));
		
		e.setCancelled(Main.isCancelled.get(e.getEventName()));
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent e) {
		Main.isCancelled.put(e.getEventName(), e.isCancelled());
		
		callMethod("onBlockBreak", e.getPlayer().getName(), blockToJSON(e.getBlock()));
		
		e.setCancelled(Main.isCancelled.get(e.getEventName()));
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent e) {
		Main.isCancelled.put(e.getEventName(), e.isCancelled());
		
		callMethod("onBlockBurn", blockToJSON(e.getBlock()));
		
		e.setCancelled(Main.isCancelled.get(e.getEventName()));
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockDamage(BlockDamageEvent e) {
		Main.isCancelled.put(e.getEventName(), e.isCancelled());
		Main.instaBreak = e.getInstaBreak();
		
		callMethod("onBlockDamage", e.getPlayer().getName(), blockToJSON(e.getBlock()));
		
		e.setCancelled(Main.isCancelled.get(e.getEventName()));
		e.setInstaBreak(Main.instaBreak);
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent e) {
		Main.isCancelled.put(e.getEventName(), e.isCancelled());
		
		callMethod("onBlockExplode", blockToJSON(e.getBlock()), (double) e.getYield());
		
		e.setCancelled(Main.isCancelled.get(e.getEventName()));
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onBlockFade(BlockFadeEvent e) {
		Main.isCancelled.put(e.getEventName(), e.isCancelled());
		
		callMethod("onBlockExplode", blockToJSON(e.getBlock()));
		e.setCancelled(Main.isCancelled.get(e.getEventName()));
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent e) {
		Main.isCancelled.put(e.getEventName(), e.isCancelled());
		
		callMethod("onPlayerInteract", e.getPlayer().getName(), e.getAction().ordinal(), blockToJSON(e.getClickedBlock()),
				itemToJSON(e.getPlayer().getInventory().getItemInMainHand()));
		
		e.setCancelled(Main.isCancelled.get(e.getEventName()));
	}

	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent e) {
		Main.isCancelled.put(e.getEventName(), e.isCancelled());
		
		callMethod("onInventoryClick", e.getWhoClicked().getName(), e.getInventory().getName(), itemToJSON(e.getCurrentItem()), e.getSlot());
		
		e.setCancelled(Main.isCancelled.get(e.getEventName()));
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
		Main.isCancelled.put("PlayerCommandEvent", e.isCancelled());
		
		String label = e.getMessage().replace("/", "").split(" ")[0];
		String[] args = {};
		
		if(e.getMessage().contains(" "))
			args = e.getMessage().substring(label.length()+2).split(" ");
		
		callMethod("onCommand", e.getPlayer().getName(), label, arrayToStr(args));
		e.setCancelled(Main.isCancelled.get("PlayerCommandEvent"));
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onServerCommand(ServerCommandEvent e) {
		Main.isCancelled.put("PlayerCommandEvent", e.isCancelled());
		
		String label = e.getCommand().replace("/", "").split(" ")[0];
		String[] args = {};
		
		if(e.getCommand().contains(" "))
			args = e.getCommand().substring(label.length()+2).split(" ");
		
		callMethod("onCommand", "<server>", label, arrayToStr(args));
		e.setCancelled(Main.isCancelled.get("PlayerCommandEvent"));
	}
}
