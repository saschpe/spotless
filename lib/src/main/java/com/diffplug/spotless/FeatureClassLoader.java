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

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;

/**
 * The requests to load a class for a feature is first delegated to the system class loader,
 * before the feature class loader searches the provided URLs. In case the class resolution or
 * verification fails, the request is delegated to the class loader of the build tool.
 * <br/>
 * Features shall be independent from build tools. Framework capabilities provided by
 * build tools can be explicitly accessed by omitting the run-time dependencies for these
 * classes in the provided URLs.
 * <br/>
 * For build tools not supporting framework capabilities required by certain features,
 * the Spotless plugin for the specific build tool provides the missing framework capabilities.
 */
public class FeatureClassLoader extends URLClassLoader {
	static {
		try {
			ClassLoader.registerAsParallelCapable();
		} catch (NoSuchMethodError ignore) {
			// Not supported on Java 6
		}
	}

	private final ClassLoader buildToolClassLoader;

	/**
	 * Constructs a new FeatureClassLoader for the given URLs, based on an {@code URLClassLoader},
	 * using the system class loader as parent. In case the class resolution or verification fails,
	 * the the class loader of the build tool is used.
	 *
	 * @param urls the URLs from which to load classes and resources
	 * @param buildToolClassLoader The build tool class loader
	 * @exception  SecurityException  If a security manager exists and prevents the creation of a class loader.
	 * @exception  NullPointerException if {@code urls} is {@code null}.
	 */

	public FeatureClassLoader(URL[] urls, ClassLoader buildToolClassLoader) {
		super(urls, null);
		Objects.requireNonNull(buildToolClassLoader);
		this.buildToolClassLoader = buildToolClassLoader;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		try {
			return super.findClass(name);
		} catch (ClassNotFoundException e) {
			if (name.startsWith("org.slf4j.")) {
				return buildToolClassLoader.loadClass(name);
			} else {
				throw e;
			}
		}
	}
}
