/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.epd.common.prototype.settings.observers;

import dk.dma.epd.common.prototype.settings.layers.IntendedRouteLayerCommonSettings;

/**
 * Interface for observing an {@link IntendedRouteLayerCommonSettings} for
 * changes.
 * 
 * @author Janus Varmarken
 */
public interface IntendedRouteLayerCommonSettingsListener extends
        RouteLayerCommonSettingsListener {

    /**
     * Invoked when the setting, specifying if intended route filter is enabled,
     * has been changed.
     * 
     * @param source
     *            The settings instance that fired this event.
     * @param useFilter
     *            {@code true} if intended route filter has been enabled,
     *            {@code false} if intended route filter has been disabled.
     */
    void isIntendedRouteFilterInUseChanged(
            IntendedRouteLayerCommonSettings<?> source, boolean useFilter);
}