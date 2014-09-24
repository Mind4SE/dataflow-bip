/**
 * Copyright (C) 2010 STMicroelectronics
 * Copyright (C) 2010 VERIMAG
 * Copyright (C) 2014 Schneider-Electric
 *
 * This file is part of "Mind Compiler"
 * The "Mind Compiler" is free software: you can redistribute 
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
 * Authors: Matthieu Leclercq, Marc Poulhiès
 * Contributors:
  * - Stephane Seyvoz - mind-compiler 2.1-SNAPSHOT support
 */

package org.ow2.mind.beam;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.fractal.adl.util.FractalADLLogManager;
import org.ow2.mind.cli.CmdOption;
import org.ow2.mind.cli.CommandLine;
import org.ow2.mind.cli.CommandOptionHandler;
import org.ow2.mind.cli.InvalidCommandLineException;

public class BackendCommandLineHandler implements CommandOptionHandler {
  public final static String BEAM_ENABLE_BIP_BACKEND = "beam-enable-bip-backend";
  
  public final static String BEAM_BIP_BACKEND_ID = "org.ow2.mind.beam.BIPBackend";
  
  protected static Logger logger = FractalADLLogManager.getLogger("beam-cli");

  public void processCommandOption(final CmdOption cmdOption, CommandLine cmdLine,
      final Map<Object, Object> context) throws InvalidCommandLineException {

    if (BEAM_BIP_BACKEND_ID.equals(cmdOption.getId()) && cmdOption.isPresent(cmdLine)) {
      logger.log(Level.INFO, "  - enabling beam bip backend'");
      context.put(BEAM_ENABLE_BIP_BACKEND, true);
    }
  }
}
