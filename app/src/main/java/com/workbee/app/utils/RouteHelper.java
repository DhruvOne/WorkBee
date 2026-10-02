package com.workbee.app.utils;

import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.List;

/**
 * RouteHelper — Generates realistic grid-based zig-zag route segments,
 * dynamic headings (bearings) for custom vehicle icons, and live traffic coloring ranges.
 */
public class RouteHelper {

    /**
     * Generates a multi-segment grid-snapped path simulating typical urban street driving
     * instead of a simple direct flight path.
     */
    public static List<LatLng> generateMockRoute(LatLng start, LatLng end) {
        List<LatLng> path = new ArrayList<>();
        if (start == null || end == null) return path;

        path.add(start);

        // Turn 1: Go partly north/south on the first avenue
        double turn1Lat = start.latitude + (end.latitude - start.latitude) * 0.35;
        double turn1Lng = start.longitude;
        path.add(new LatLng(turn1Lat, turn1Lng));

        // Turn 2: Turn east/west to cross town along a major street
        double turn2Lat = turn1Lat;
        double turn2Lng = start.longitude + (end.longitude - start.longitude) * 0.70;
        path.add(new LatLng(turn2Lat, turn2Lng));

        // Turn 3: Turn north/south to align with final avenue
        double turn3Lat = end.latitude;
        double turn3Lng = turn2Lng;
        path.add(new LatLng(turn3Lat, turn3Lng));

        // Final Destination
        path.add(end);

        return path;
    }

    /**
     * Computes the heading/bearing angle between two locations in degrees (0 - 360).
     */
    public static float calculateBearing(LatLng from, LatLng to) {
        if (from == null || to == null) return 0f;

        double lat1 = Math.toRadians(from.latitude);
        double lng1 = Math.toRadians(from.longitude);
        double lat2 = Math.toRadians(to.latitude);
        double lng2 = Math.toRadians(to.longitude);

        double dLng = lng2 - lng1;
        double y = Math.sin(dLng) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);

        double bearing = Math.atan2(y, x);
        return (float) ((Math.toDegrees(bearing) + 360) % 360);
    }

    /**
     * Interpolates along the multi-segmented path based on the current progress fraction (0.0 to 1.0).
     */
    public static LatLng getPositionAtFraction(List<LatLng> route, float fraction) {
        if (route == null || route.isEmpty()) return new LatLng(0, 0);
        if (route.size() == 1) return route.get(0);
        if (fraction <= 0f) return route.get(0);
        if (fraction >= 1f) return route.get(route.size() - 1);

        int numSegments = route.size() - 1;
        float segmentSize = 1.0f / numSegments;
        int currentSegment = (int) (fraction / segmentSize);
        if (currentSegment >= numSegments) currentSegment = numSegments - 1;

        float segmentFraction = (fraction - (currentSegment * segmentSize)) / segmentSize;
        LatLng p1 = route.get(currentSegment);
        LatLng p2 = route.get(currentSegment + 1);

        double lat = p1.latitude + (p2.latitude - p1.latitude) * segmentFraction;
        double lng = p1.longitude + (p2.longitude - p1.longitude) * segmentFraction;
        return new LatLng(lat, lng);
    }

    /**
     * Renders a human-readable banner explaining current simulated traffic conditions.
     */
    public static String getTrafficBannerText(float fraction) {
        if (fraction < 0.35f) {
            return "🚦 Traffic is clear. Making swift progress! ⚡";
        } else if (fraction < 0.70f) {
            return "⚠️ Moderate congestion near Blossom Avenue. ETA +1 min";
        } else {
            return "🚗 Dense urban traffic. Approaching final street carefully.";
        }
    }

    /**
     * Returns a color HEX string representing active traffic load on a segment.
     */
    public static String getTrafficColorForSegment(int segmentIndex) {
        switch (segmentIndex) {
            case 0: return "#4CAF50"; // Green (clear)
            case 1: return "#FFC107"; // Amber (moderate)
            case 2: return "#F44336"; // Red (heavy)
            default: return "#FFC107";
        }
    }
}
