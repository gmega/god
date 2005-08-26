package ddproto1.configurator;

import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.InvalidAttributeValueException;

public interface IWritableConfigurable {
	public void setAttribute(String key, String val) throws IllegalAttributeException, InvalidAttributeValueException;
}
