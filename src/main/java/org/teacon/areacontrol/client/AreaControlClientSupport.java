package org.teacon.areacontrol.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.network.ACNetworking;
import org.teacon.areacontrol.network.ACPingServer;

import java.util.Collections;
import java.util.List;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = "area_control", value = Dist.CLIENT)
public final class AreaControlClientSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");
    private static final Marker MARKER = MarkerFactory.getMarker("Client");

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        LOGGER.info(MARKER, "AreaControl is installed on client; enabling enhanced client support");

        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ClientPlayerNetworkEvent.LoggingIn.class,
                AreaControlClientSupport::afterLogin);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class,
                AreaControlClientSupport::resetNearbyAreas);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, RenderLevelStageEvent.class,
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
    public static volatile BlockPos selectionMin, selectionMax;

    static void renderAreaBorder(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        final Minecraft mc = Minecraft.getInstance();
        final var transform = event.getPoseStack();
        final var camera = mc.getEntityRenderDispatcher().camera;
        if (camera == null) {
            // BloCamLimb said this can be null when OptiFine is installed.
            return;
        }
        transform.pushPose();
        final var proj = camera.getPosition();
        transform.translate(-proj.x, -proj.y, -proj.z);

        var buffers = mc.renderBuffers().bufferSource();

        var builder = buffers.getBuffer(Holder.BORDER);

        var renderDistance = mc.options.getEffectiveRenderDistance() * 16;
        BlockPos playerPos;
        if (mc.player != null) {
            playerPos = mc.player.blockPosition();
        } else {
            playerPos = BlockPos.ZERO;
        }
        if (System.currentTimeMillis() < knownAreasExpiresAt) {
            for (var area : knownAreas) {
                if (playerPos.closerThan(new Vec3i(area.midX, area.midY, area.midZ), renderDistance)) {
                    int minY = Math.max(-128, area.minY);
                    int maxY = Math.min(320, area.maxY);
                    box(transform, builder, area.enclosed ? 0x8826619C : 0x887FFFD4, area.minX, minY, area.minZ, area.maxX + 1, maxY + 1, area.maxZ + 1);
                }
            }
        }
        if (selectionMin != null && selectionMax != null) {
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

    private static final class Holder extends RenderStateShard {

        static ShaderInstance areaControlShader;

        private Holder(String name, Runnable setupCallback, Runnable cleanupCallback) {
            super(name, setupCallback, cleanupCallback);
        }

        //static final ShaderStateShard AREA_CONTROL_SHADER = new ShaderStateShard(() -> areaControlShader);

        // This is the vanilla world border texture; we are merely referring it, but using a custom texture state.
        static final EmptyTextureStateShard REPEATED_FORCE_FIELD = new TextureStateShard(ResourceLocation.withDefaultNamespace("textures/misc/forcefield.png"), false, false);

        static final RenderType BORDER = RenderType.create("area_control_border",
                DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_TEX_LIGHTMAP_SHADER) // Must be here
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setTextureState(REPEATED_FORCE_FIELD)
                        // 海螺 told me that vanilla avoids z-fighting during world border rendering
                        // by RenderSystem.enablePolygonOffset(), so here it is...
                        .setLayeringState(POLYGON_OFFSET_LAYERING)
                        .createCompositeState(false));

    }
}
