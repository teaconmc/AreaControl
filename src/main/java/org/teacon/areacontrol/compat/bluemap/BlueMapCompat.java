package org.teacon.areacontrol.compat.bluemap;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.HtmlMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import org.teacon.areacontrol.AreaManager;
import org.teacon.areacontrol.api.Area;

public class BlueMapCompat {

    public static void init() {
        BlueMapAPI.onEnable(api -> {
            for (var blueMapWorld : api.getWorlds()) {
                for (var map : blueMapWorld.getMaps()) {
                    map.getMarkerSets().put("AreaControl", populateMarkerSetFor(blueMapWorld.getId()));
                }
            }
        });
    }

    public static MarkerSet populateMarkerSetFor(String worldId) {
        // Extract the real level key out of worldId
        if (worldId.indexOf('#') > -1) {
            worldId = worldId.substring(worldId.indexOf('#') + 1);
        }
        MarkerSet markers = new MarkerSet("AreaControl", true, false);
        for (Area area : AreaManager.INSTANCE.findAllIn(worldId)) {
            // Make sure you go through every vertex and come back to the starting point.
            // Also make sure you follow only one order. Here we follow clock-wise order.
            // Also note that this position is not block-aligned, so remember to add extra 1 to maxX/Y/Z.
            var xzPlane = new Shape(
                    new Vector2d(area.minX, area.minZ), new Vector2d(area.minX, area.maxZ + 1),
                    new Vector2d(area.maxX + 1, area.maxZ + 1), new Vector2d(area.maxX + 1, area.minZ),
                    new Vector2d(area.minX, area.minZ));
            var marker =  new ExtrudeMarker(area.name, xzPlane, area.minY, area.maxY + 1);
            marker.setFillColor(area.resolveParent() != null ? AZURE : AQUAMARINE);
            marker.setListed(true);
            marker.setDetail(area.name);
            markers.put(area.uid.toString(), marker);

            var areaCenter = new Vector3d((area.maxX + 1 + area.minX) / 2.0, (area.maxY + 1 + area.minY) / 2.0, (area.maxZ + 1 + area.minZ) / 2.0);
            var nameMarker = new HtmlMarker(area.name, areaCenter, "<h1 style=\"text-align: center;\">" + area.name + "</h1>");
            nameMarker.setListed(false);
            markers.put(area.uid + "-name-label", nameMarker);
        }
        return markers;
    }

    private static final Color AZURE = new Color(0x8826619C); // Alpha 0x88, RGB #26619C
    private static final Color AQUAMARINE = new Color(0x887FFFD4); // Alpha 0x88, RGB #7FFFD4
}
