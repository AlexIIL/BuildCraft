package buildcraft.api.core;

import java.util.Collection;

import com.google.common.collect.Lists;

import net.minecraft.block.properties.PropertyEnum;
import net.minecraftforge.common.property.IUnlistedProperty;

public class PropertyUnlistedEnum<T extends Enum<T>> extends PropertyEnum implements IUnlistedProperty<T> {

	public PropertyUnlistedEnum(String name, Class<T> valueClass) {
		this(name, valueClass, Lists.newArrayList(valueClass.getEnumConstants()));
	}

	public PropertyUnlistedEnum(String name, Class<T> valueClass, Collection<T> allowedValues) {
		super(name, valueClass, allowedValues);
	}

	@Override
	public boolean isValid(T value) {
		return false;
	}

	@Override
	public Class<T> getType() {
		return null;
	}

	@Override
	public String valueToString(T value) {
		return null;
	}
}
