package org.teacon.areacontrol.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.network.ACNetworking;
import org.teacon.areacontrol.network.ACPingServer;

import java.util.Collections;
import java.util.List;

@Mod(value = "area_control", dist = Dist.CLIENT)
public final class AreaControlClientSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");
    private static final Marker MARKER = MarkerFactory.getMarker("Client");

    public AreaControlClientSupport(ModContainer container) {
        LOGGER.info(MARKER, "AreaControl is installed on client; enabling enhanced client support");

        container.registerExtensionPoint(IConfigScreenFactory.class, (mc, parent) -> new ConfigurationScreen(container, parent));

        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ClientPlayerNetworkEvent.LoggingIn.class,
                AreaControlClientSupport::afterLogin);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class,
                AreaControlClientSupport::resetNearbyAreas);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, RenderLevelStageEvent.AfterTranslucentBlocks.class,
                AreaControlClientSupport::renderAreaBorder);
    }

    static void resetNearbyAreas(EntityJoinLevelEvent event) {
        // If player enters a new level, reset nearby areas
        if (event.getLevel().isClientSide() && event.getEntity() instanceof Player) {
            knownAreas = Collections.emptyList();
            knownAreasExpiresAt = 0L;
        }
    }

    static void afterLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        ACNetworking.send(new ACPingServer());
    }

    public static volatile List<Area.Summary> knownAreas = Collections.emptyList();
    public static volatile long knownAreasExpiresAt = 0L;
    public static volatile ResourceKey<Level> selectionDimension;
    public static volatile BlockPos selectionMin, selectionMax;

    static void renderAreaBorder(RenderLevelStageEvent.AfterTranslucentBlocks event) {
        final Minecraft mc = Minecraft.getInstance();
        final var transform = event.getPoseStack();
        final var camera = mc.getEntityRenderDispatcher().camera;
        if (camera == null) {
            // BloCamLimb said this can be null when OptiFine is installed.
            return;
        }
        transform.pushPose();
        final var proj = camera.position();
        transform.translate(-proj.x, -proj.y, -proj.z);

        var buffers = mc.renderBuffers().bufferSource();

        var builder = buffers.getBuffer(AreaControlRenderTypes.BORDER);

        var renderDistance = mc.options.getEffectiveRenderDistance() * 16;
        BlockPos playerPos;
        if (mc.player != null) {
            playerPos = mc.player.blockPosition();
        } else {
            playerPos = BlockPos.ZERO;
        }
//        if (System.currentTimeMillis() < knownAreasExpiresAt) {
            for (var area : knownAreas) {
                //xkball: 只计算xz平面上的距离, 不然视距小的时候会有错误效果
                if (new Vector2f(playerPos.getX(), playerPos.getZ()).distance(new Vector2f(area.midX, area.midZ)) < renderDistance) {
                    int minY = Math.max(-128, area.minY);
                    int maxY = Math.min(320, area.maxY);
                    box(transform, builder, area.enclosed ? 0x8826619C : 0x887FFFD4, area.minX, minY, area.minZ, area.maxX + 1, maxY + 1, area.maxZ + 1);
                }
            }
//        }
        var level = mc.level;
        if (level != null && level.dimension() == selectionDimension && selectionMin != null && selectionMax != null) {
            box(transform, builder, 0xFFFFD700, selectionMin.getX(), selectionMin.getY(), selectionMin.getZ(), selectionMax.getX() + 1, selectionMax.getY() + 1, selectionMax.getZ() + 1);
        }

        buffers.endBatch();

        transform.popPose();
    }

    static void box(PoseStack pose, VertexConsumer vertexConsumer, int argbColor,
                    int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int diffX = maxX - minX;
        int diffY = maxY - minY;
        int diffZ = maxZ - minZ;
        var x = pose.last().pose();

        // Bottom interior
        vertexConsumer.addVertex(x, minX, minY, minZ).setColor(argbColor).setUv(0, 0);

        vertexConsumer.addVertex(x, minX, minY, maxZ).setColor(argbColor).setUv(0, diffZ);
        vertexConsumer.addVertex(x, maxX, minY, maxZ).setColor(argbColor).setUv(diffX, diffZ);
        vertexConsumer.addVertex(x, maxX, minY, minZ).setColor(argbColor).setUv(diffX, 0);

        // Bottom exterior
        vertexConsumer.addVertex(x, minX, minY, minZ).setColor(argbColor).setUv(0, 0);
        vertexConsumer.addVertex(x, maxX, minY, minZ).setColor(argbColor).setUv(0, diffX);
        vertexConsumer.addVertex(x, maxX, minY, maxZ).setColor(argbColor).setUv(diffZ, diffX);
        vertexConsumer.addVertex(x, minX, minY, maxZ).setColor(argbColor).setUv(diffZ, 0);

        // Top interior
        vertexConsumer.addVertex(x, minX, maxY, minZ).setColor(argbColor).setUv(0, 0);
        vertexConsumer.addVertex(x, maxX, maxY, minZ).setColor(argbColor).setUv(0, diffX);
        vertexConsumer.addVertex(x, maxX, maxY, maxZ).setColor(argbColor).setUv(diffZ, diffX);
        vertexConsumer.addVertex(x, minX, maxY, maxZ).setColor(argbColor).setUv(diffZ, 0);

        // Top exterior
        vertexConsumer.addVertex(x, minX, maxY, minZ).setColor(argbColor).setUv(0, 0);
        vertexConsumer.addVertex(x, minX, maxY, maxZ).setColor(argbColor).setUv(0, diffZ);
        vertexConsumer.addVertex(x, maxX, maxY, maxZ).setColor(argbColor).setUv(diffX, diffZ);
        vertexConsumer.addVertex(x, maxX, maxY, minZ).setColor(argbColor).setUv(diffX, 0);

        // Front interior
        vertexConsumer.addVertex(x, minX, minY, maxZ).setColor(argbColor).setUv(0, 0);
        vertexConsumer.addVertex(x, minX, maxY, maxZ).setColor(argbColor).setUv(0, diffY);
        vertexConsumer.addVertex(x, maxX, maxY, maxZ).setColor(argbColor).setUv(diffX, diffY);
        vertexConsumer.addVertex(x, maxX, minY, maxZ).setColor(argbColor).setUv(diffX, 0);

        // Front exterior
        vertexConsumer.addVertex(x, minX, minY, maxZ).setColor(argbColor).setUv(0, 0);
        vertexConsumer.addVertex(x, maxX, minY, maxZ).setColor(argbColor).setUv(0, diffX);
        vertexConsumer.addVertex(x, maxX, maxY, maxZ).setColor(argbColor).setUv(diffY, diffX);
        vertexConsumer.addVertex(x, minX, maxY, maxZ).setColor(argbColor).setUv(diffY, 0);

        // Back interior
        vertexConsumer.addVertex(x, minX, minY, minZ).setColor(argbColor).setUv(0, 0);
        vertexConsumer.addVertex(x, maxX, minY, minZ).setColor(argbColor).setUv(0, diffX);
        vertexConsumer.addVertex(x, maxX, maxY, minZ).setColor(argbColor).setUv(diffY, diffX);
        vertexConsumer.addVertex(x, minX, maxY, minZ).setColor(argbColor).setUv(diffY, 0);

        // Back exterior
        vertexConsumer.addVertex(x, minX, minY, minZ).setColor(argbColor).setUv(0, 0);
        vertexConsumer.addVertex(x, minX, maxY, minZ).setColor(argbColor).setUv(0, diffY);
        vertexConsumer.addVertex(x, maxX, maxY, minZ).setColor(argbColor).setUv(diffX, diffY);
        vertexConsumer.addVertex(x, maxX, minY, minZ).setColor(argbColor).setUv(diffX, 0);

        // Left interior
        vertexConsumer.addVertex(x, minX, minY, minZ).setColor(argbColor).setUv(0, 0);
        vertexConsumer.addVertex(x, minX, maxY, minZ).setColor(argbColor).setUv(0, diffY);
        vertexConsumer.addVertex(x, minX, maxY, maxZ).setColor(argbColor).setUv(diffZ, diffY);
        vertexConsumer.addVertex(x, minX, minY, maxZ).setColor(argbColor).setUv(diffZ, 0);

        // Left exterior
        vertexConsumer.addVertex(x, minX, minY, minZ).setColor(argbColor).setUv(0, 0);
        vertexConsumer.addVertex(x, minX, minY, maxZ).setColor(argbColor).setUv(0, diffZ);
        vertexConsumer.addVertex(x, minX, maxY, maxZ).setColor(argbColor).setUv(diffY, diffZ);
        vertexConsumer.addVertex(x, minX, maxY, minZ).setColor(argbColor).setUv(diffY, 0);

        // Right interior
        vertexConsumer.addVertex(x, maxX, minY, minZ).setColor(argbColor).setUv(0, 0);
        vertexConsumer.addVertex(x, maxX, minY, maxZ).setColor(argbColor).setUv(0, diffZ);
        vertexConsumer.addVertex(x, maxX, maxY, maxZ).setColor(argbColor).setUv(diffY, diffZ);
        vertexConsumer.addVertex(x, maxX, maxY, minZ).setColor(argbColor).setUv(diffY, 0);

        // Right exterior
        vertexConsumer.addVertex(x, maxX, minY, minZ).setColor(argbColor).setUv(0, 0);
        vertexConsumer.addVertex(x, maxX, maxY, minZ).setColor(argbColor).setUv(0, diffY);
        vertexConsumer.addVertex(x, maxX, maxY, maxZ).setColor(argbColor).setUv(diffZ, diffY);
        vertexConsumer.addVertex(x, maxX, minY, maxZ).setColor(argbColor).setUv(diffZ, 0);
    }
}
