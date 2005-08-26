package ddproto1.configurator;

import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.UninitializedAttributeException;

public interface IReadableConfigurable {
	public String getAttribute(String key) throws IllegalAttributeException, UninitializedAttributeException;
}
