package org.jakub1221.herobrineai.NPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.v1_11_R1.Entity;
import net.minecraft.server.v1_11_R1.PlayerInteractManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jakub1221.herobrineai.HerobrineAI;
import org.jakub1221.herobrineai.NPC.Entity.HumanEntity;
import org.jakub1221.herobrineai.NPC.Entity.HumanNPC;
import org.jakub1221.herobrineai.NPC.NMS.BServer;
import org.jakub1221.herobrineai.NPC.NMS.BWorld;
import org.jakub1221.herobrineai.NPC.Network.NetworkCore;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

public class NPCCore {

	private ArrayList<HumanNPC> npcs = new ArrayList<HumanNPC>();
	private BServer server;
	private int taskid;
	private Map<World, BWorld> bworlds = new HashMap<World, BWorld>();
	private NetworkCore networkCore;
	public static JavaPlugin plugin;
	public boolean isInLoaded = false;
	private int lastID = 0;

	private GameProfile HerobrineGameProfile = getHerobrineGameProfile();

	private GameProfile getHerobrineGameProfile() {
		GameProfile profile = new GameProfile(
											  UUID.fromString(HerobrineAI.getPluginCore().getConfigDB().HerobrineUUID),
											  HerobrineAI.getPluginCore().getConfigDB().HerobrineName
											  );
		
		Property textures = new Property("textures",
				"eyJ0aW1lc3RhbXAiOjE1MDQ5ODE0MTMwMTAsInByb2ZpbGVJZCI6Ijk1ODZlNWFiMTU3YTQ2NThhZDgwYjA3NTUyYTljYTYzIiwicHJvZmlsZU5hbWUiOiJNSEZfSGVyb2JyaW5lIiwic2lnbmF0dXJlUmVxdWlyZWQiOnRydWUsInRleHR1cmVzIjp7IlNLSU4iOnsidXJsIjoiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85OGI3Y2EzYzdkMzE0YTYxYWJlZDhmYzE4ZDc5N2ZjMzBiNmVmYzg0NDU0MjVjNGUyNTA5OTdlNTJlNmNiIn19fQ==",
				"B4Iee2jf6nEWj1BqGkZYzL/OIHn7400qQZiH1wYN8oaFZly7IoC3fBFKDM8y2+KVQn0BnJL1mOffFhsZ9rhkzOQsfhsVgvTardPB6j2gq18Vx47+Ni+mi7SdDL+yug/xchzKFppDUcsfsy6zzOCL7qFxPVe4jM2P9yPZv0GNwcaECK5UWY26LAkHK+PuSBS9CWzwr+Yu232K10kRddBIIfAaLhP9XHFI9LdXa30SL4PYfAPh3fbh6OW0+PQOHMJTzHr5m4hytY+cu6zWxaDfmEpGIa0wO8uFMxkSRSR1aTYYLuakb+ChiOl0wd5YvMiELI9sorOIMN72DkInOJ4PGGZAFIXJeGJbp1NceZhTtNK6W4o02sH2/avdNEwq+sH3IZs78xTKhy9WhLz2XhJSPN9t5wev6z3U+ellkfsrmgkF7vb2GRAjHi2YQ2ASiIoODo20g0OXTYd5914TxNXPz8EU3Jc3cvxTDNTSOhJPi63lRlw873Uv4mq9yiL730D1JVrZaElx3STZehptM7xY00H32gQA3MLMdteM/7Btm7Yc89mzC6KmHKCFRyKEtwexIfgkHzTTHxXGfPQS4JuqaNN+EVwlh1r7p2GGNMWioctG5P6jiYPKpN83WIAvydmPEkqw9Diutttw7hOPtU9F/3/U+AUL6FwVQ3eef/uO0eU=");
		
		profile.getProperties().put(textures.getName(), textures);
		
		return profile;
	}

	public NPCCore(JavaPlugin plugin) {
		
		server = BServer.getInstance();
		
		networkCore = new NetworkCore();
		
		
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(HerobrineAI.getPluginCore(), new Runnable() {
			@Override
			public void run() {
				final ArrayList<HumanNPC> toRemove = new ArrayList<HumanNPC>();
				for (final HumanNPC humanNPC : npcs) {
					final Entity entity = humanNPC.getEntity();
					if (entity.dead) {
						toRemove.add(humanNPC);
					}
				}
				for (final HumanNPC n : toRemove) {
					npcs.remove(n);
				}
			}
		}, 1L, 1L);
		
		this.HerobrineGameProfile = getHerobrineGameProfile();
	}

	public void removeAll() {
		for (HumanNPC humannpc : npcs) {
			if (humannpc != null) {
				humannpc.removeFromWorld();
			}
		}
		npcs.clear();
	}

	public BWorld getBWorld(World world) {
		BWorld bworld = bworlds.get(world);
		if (bworld != null) {
			return bworld;
		}
		bworld = new BWorld(world);
		bworlds.put(world, bworld);
		return bworld;
	}

	public void DisableTask() {
		Bukkit.getServer().getScheduler().cancelTask(taskid);
	}

	private class WorldL implements Listener {
		@SuppressWarnings("unused")
		@EventHandler
		public void onChunkLoad(ChunkLoadEvent event) throws EventException {
			for (HumanNPC humannpc : npcs) {
				if (humannpc != null
						&& event.getChunk() == humannpc.getBukkitEntity().getLocation().getBlock().getChunk()) {

					if (isInLoaded == false) {
						BWorld world = getBWorld(event.getWorld());

						isInLoaded = true;
					}
				}
			}

		}

		@EventHandler
		public void onChunkUnload(ChunkUnloadEvent event) {
			for (HumanNPC humannpc : npcs) {
				if (humannpc != null
						&& event.getChunk() == humannpc.getBukkitEntity().getLocation().getBlock().getChunk()) {

				}
			}
		}
	}

	public HumanNPC spawnHumanNPC(String name, Location l) {
		lastID++;
		int id = lastID;
		return spawnHumanNPC(name, l, id);
	}

	public HumanNPC spawnHumanNPC(String name, Location l, int id) {

		final BWorld world = server.getWorld(l.getWorld().getName());
		final HumanEntity humanEntity = new HumanEntity(this, world, HerobrineGameProfile, new PlayerInteractManager(world.getWorldServer()));		
		humanEntity.setLocation(l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
		world.getWorldServer().addEntity(humanEntity);
		final HumanNPC humannpc = new HumanNPC(humanEntity, id);
		npcs.add(humannpc);
		
		return humannpc;
	}

	public HumanNPC getHumanNPC(int id) {

		for (HumanNPC n : npcs) {
			if (n.getID() == id) {
				return n;
			}
		}

		return null;
	}

	public BServer getServer() {
		return server;
	}

	public NetworkCore getNetworkCore() {
		return networkCore;
	}

}
