package org.jboss.jdf.stacks.parser;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;

public class CustomClassloaderConstructor extends Constructor {

	private final ClassLoader loader;

	public CustomClassloaderConstructor(ClassLoader cLoader) {
		this(Object.class, cLoader);
	}

	public CustomClassloaderConstructor(Class<? extends Object> theRoot, ClassLoader theLoader) {
		super(theRoot, loaderOptions());
		if (theLoader == null) {
			throw new NullPointerException("Loader must be provided.");
		}
		this.loader = theLoader;
	}

	private static LoaderOptions loaderOptions() {
		LoaderOptions options = new LoaderOptions();
		options.setMaxAliasesForCollections(5000);
		return options;
	}

	@Override
	protected Class<?> getClassForName(String name) throws ClassNotFoundException {
		return Class.forName(name, true, loader);
	}
}
