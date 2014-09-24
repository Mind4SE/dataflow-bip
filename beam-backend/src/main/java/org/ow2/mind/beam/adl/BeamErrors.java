/**
 * Copyright (C) 2010 VERIMAG
 *
 * This file is part of "Mind Compiler" is free software: you can redistribute 
 * it and/or modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact: mind@ow2.org
 *
 * Authors: Marc Poulhiès
 * Contributors: 
 */
package org.ow2.mind.beam.adl;

import org.objectweb.fractal.adl.error.ErrorTemplate;

import static org.objectweb.fractal.adl.error.ErrorTemplateValidator.validErrorTemplate;

/**
 * {@link ErrorTemplate} for annotation support.
 */
public enum BeamErrors implements ErrorTemplate {
    BEAM_ERROR(0, "Generic Beam error."),
    ;

    /** The groupId of ErrorTemplates defined in this enumeration. */
    public static final String GROUP_ID = "BEAM";

    private int id;
    private String format;
    
    private BeamErrors(final int id, final String message){
        this.id = id;
        this.format = message;
        assert validErrorTemplate(this);

    }
    
    public int getErrorId() {
        return id;
    }

    public String getFormat() {
        return format;
    }

    public String getFormatedMessage(Object... args) {
        return format;
    }

    public String getGroupId() {
        return GROUP_ID;
    }

}
