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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.fractal.adl.util.FractalADLLogManager;
import org.ow2.mind.cli.CommandOptionHandler;
import org.ow2.mind.cli.CmdArgument;
import org.ow2.mind.cli.CmdOption;
import org.ow2.mind.cli.CommandLine;
import org.ow2.mind.cli.InvalidCommandLineException;

public class CommandLineHandler implements CommandOptionHandler {
	public final static String BEAM_CLI_GEN_BIP = "beam-generate-bip";
	public final static String BEAM_EXEC_DEBUG_MAP = "beam-exec-debug-map";

	public final static String BEAM_DEBUG_ID = "org.ow2.mind.beam.Debug"; 
	public final static String BEAM_BIP_OUTPUT_ID = "org.ow2.mind.beam.BIPOutput";
	
	protected static Logger logger = FractalADLLogManager.getLogger("beam-cli");

	public void processCommandOption(final CmdOption cmdOption, CommandLine cmdLine,
			final Map<Object, Object> context) throws InvalidCommandLineException {

		if (BEAM_BIP_OUTPUT_ID.equals(cmdOption.getId()) && cmdOption.isPresent(cmdLine)) {
			logger.log(Level.INFO, "  - setting 'only bip'");
			context.put(BEAM_CLI_GEN_BIP, true);
		} else if (BEAM_DEBUG_ID.equals(cmdOption.getId()) && cmdOption.isPresent(cmdLine)){
			Set<String> debug_exec_map = (Set<String>) context.get(BEAM_EXEC_DEBUG_MAP);
			if (debug_exec_map == null){
				debug_exec_map = new HashSet<String>();
				context.put(BEAM_EXEC_DEBUG_MAP, debug_exec_map);
			}
			String value = ((CmdArgument)cmdOption).getValue(cmdLine);
			if (value != null) {
				String[] all_debug = value.split(":");
				for (String debug_item : all_debug){
					debug_exec_map.add(debug_item);
				}
			}
		}



	}
}
