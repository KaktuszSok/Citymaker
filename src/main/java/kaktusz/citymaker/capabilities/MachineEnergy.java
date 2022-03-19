package kaktusz.citymaker.capabilities;

import kaktusz.citymaker.init.ModConfig;
import net.minecraftforge.energy.EnergyStorage;

public class MachineEnergy extends EnergyStorage {

	public MachineEnergy(int capacity) {
		super(capacity, capacity, 0);
	}

	public boolean canConsumeEnergy(int amount) {
		return energy >= amount;
	}

	public boolean tryConsumeEnergy(int amount) {
		if(energy < amount)
			return false;

		energy -= amount;
		return true;
	}
}
