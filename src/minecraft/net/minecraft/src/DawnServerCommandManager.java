package net.minecraft.src;

import java.util.ArrayList;

public class DawnServerCommandManager extends ServerCommandManager {
	private static ArrayList<ICommand> commands = new ArrayList<ICommand>();
	
	public DawnServerCommandManager() {
		super();
		
		for (ICommand command : commands) {
			this.registerCommand(command);
		}
	}
	
	public static void registerAddonCommand(ICommand command) {
		commands.add(command);
	}
}