/* Copyright (c) 2011 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dk.dma.epd.shore.voyage;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * Different events for routes
 */
public enum VoyageUpdateEvent {
    VOYAGE_CHANGED, VOYAGE_ADDED, VOYAGE_REMOVED, VOYAGE_VISIBILITY_CHANGED, VOYAGE_WAYPOINT_DELETED, VOYAGE_WAYPOINT_APPENDED, VOYAGE_WAYPOINT_MOVED, VOYAGE_PENDING;

    public boolean is(VoyageUpdateEvent... events) {
        return EnumSet.copyOf(Arrays.asList(events)).contains(this);
    }
};
