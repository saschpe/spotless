/*
 * Copyright 2016 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.spotless;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Objects;

import javax.annotation.Nullable;

/** Class loader attempts the local URLs first before trying parent class loader */
public class LocalFirstClassLoader extends URLClassLoader {
	private final ClassLoader parent;

	/**
	 * Constructs a new URLClassLoader for the given URLs. The URLs will be
	 * searched in the order specified for classes and resources before
	 * searching in the specified parent class loader. Any URL that ends with
	 * a '/' is assumed to refer to a directory. Otherwise, the URL is assumed
	 * to refer to a JAR file which will be downloaded and opened as needed.
	 *
	 * <p>If there is a security manager, this method first
	 * calls the security manager's {@code checkCreateClassLoader} method
	 * to ensure creation of a class loader is allowed.
	 *
	 * @param urls the URLs from which to load classes and resources
	 * @param parent the parent class loader for delegation
	 * @exception  SecurityException  if a security manager exists and its
	 *             {@code checkCreateClassLoader} method doesn't allow
	 *             creation of a class loader.
	 * @exception  NullPointerException if {@code urls} is {@code null}.
	 * @see SecurityManager#checkCreateClassLoader
	 */
	public LocalFirstClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, null);
		Objects.requireNonNull(parent);
		this.parent = parent;
	}

	@Override
	public URL getResource(String name) {
		return applyParentIfResultIsNull(name,
				(String s) -> super.getResource(s),
				(String s) -> parent.getResource(s));
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return applyParentIfClassNotFound(name,
				(String s) -> super.loadClass(s),
				(String s) -> parent.loadClass(s));
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		return applyParentIfResultIsNull(name,
				(String s) -> super.getResourceAsStream(s),
				(String s) -> parent.getResourceAsStream(s));
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		return applyParentIfResultIsEmpty(name,
				(String s) -> super.getResources(s),
				(String s) -> parent.getResources(s));
	}

	private static interface FunctionEx<T, R, E extends Exception> {
		public @Nullable R apply(@Nullable T t) throws E;
	}

	private static @Nullable <T, R, E extends Exception> R applyParentIfResultIsNull(T parameter, FunctionEx<T, R, E> local, FunctionEx<T, R, E> parent) throws E {
		R result = local.apply(parameter);
		if (null == result) {
			result = parent.apply(parameter);
		}
		return result;
	}

	private static <T, R extends Enumeration<?>, E extends Exception> R applyParentIfResultIsEmpty(T parameter, FunctionEx<T, R, E> local, FunctionEx<T, R, E> parent) throws E {
		R result = local.apply(parameter);
		if (!result.hasMoreElements()) {
			result = parent.apply(parameter);
		}
		return result;
	}

	private static <T, R> R applyParentIfClassNotFound(T parameter, FunctionEx<T, R, ClassNotFoundException> local, FunctionEx<T, R, ClassNotFoundException> parent) throws ClassNotFoundException {
		try {
			return local.apply(parameter);
		} catch (ClassNotFoundException e) {
			return parent.apply(parameter);
		}
	}
}
