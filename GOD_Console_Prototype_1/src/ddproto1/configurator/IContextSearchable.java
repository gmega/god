package ddproto1.configurator;

import java.util.Set;

import ddproto1.exception.DuplicateSymbolException;
import ddproto1.exception.NoSuchSymbolException;

public interface IContextSearchable {
	public void addAsParent(IObjectSpec parent) throws DuplicateSymbolException;
    public void removeParent(IObjectSpec parent) throws NoSuchSymbolException;
    public Set<IObjectSpec> getAllParents();
}
