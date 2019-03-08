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
package com.diffplug.spotless.kotlin;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.diffplug.spotless.*;

/**
 * Wraps up [detekt](https://github.com/arturbosch/detekt) as a FormatterStep.
 */
public class DetektStep {
	private DetektStep() {
	}

	private static final String DEFAULT_VERSION = "1.0.0-RC14";
	private static final String NAME = "detekt";
	private static final String MAVEN_COORDINATE = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:";

	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	public static FormatterStep create(String version, Provisioner provisioner) {
		return create(version, provisioner, "");
	}

	public static FormatterStep create(String version, Provisioner provisioner, String configFile) {
		return create(version, provisioner, false, configFile);
	}

	public static FormatterStep createForScript(String version, Provisioner provisioner) {
		return create(version, provisioner, true, "");
	}

	public static FormatterStep createForScript(String version, Provisioner provisioner, String configFile) {
		return create(version, provisioner, true, configFile);
	}

	private static FormatterStep create(String version, Provisioner provisioner, boolean isScript, String configFile) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
			() -> new DetektStep.State(version, provisioner, isScript, configFile),
			DetektStep.State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	static final class State implements Serializable {
		/**
		 * Are the files being linted Kotlin script files.
		 */
		private final boolean isScript;
		private final String configFile;
		/**
		 * The jar that contains the eclipse formatter.
		 */
		final JarState jarState;

		State(String version, Provisioner provisioner, boolean isScript, String configFile) throws IOException {
			this.configFile = configFile;
			this.isScript = isScript;
			this.jarState = JarState.from(MAVEN_COORDINATE + version, provisioner);
		}

		FormatterFunc createFormat() throws Exception {
			ClassLoader classLoader = jarState.getClassLoader();

			Class<?> detektFacadeClass = classLoader.loadClass("io.gitlab.arturbosch.detekt.core.DetektFacade");
			Object detektFacadeCompanion = detektFacadeClass.getDeclaredField("INSTANCE").get(null);

			Class<?> processSettingsClass = classLoader.loadClass("io.gitlab.arturbosch.detekt.core.ProcessingSettings");
			Method createMethod = detektFacadeClass.getMethod("create", processSettingsClass);

			// TODO: Parse config file...
			//Class<?> configClass = classLoader.loadClass("io.gitlab.arturbosch.detekt.api.Config");

			Constructor processSettingsClassConstructor = processSettingsClass.getDeclaredConstructor(List.class);

			// TODO: Paths...
			ArrayList<Path> paths = new ArrayList<>();

			Object processSettings = processSettingsClassConstructor.newInstance(paths);

			// Create 'DetektFacade' instance..
			Object detektFacade = createMethod.invoke(detektFacadeCompanion, processSettings);


			return input -> {
				Object detektion = detektFacadeClass.getMethod("run").invoke(null);

				// TODO: Convert to printable string..
				return "";
			};

			/*// first, we get the standard rules
			Class<?> standardRuleSetProviderClass = classLoader.loadClass("com.github.shyiko.ktlint.ruleset.standard.StandardRuleSetProvider");
			Object standardRuleSet = standardRuleSetProviderClass.getMethod("get").invoke(standardRuleSetProviderClass.newInstance());
			Iterable<?> ruleSets = Collections.singletonList(standardRuleSet);

			// next, we create an error callback which throws an assertion error when the format is bad
			Class<?> function2Interface = classLoader.loadClass("kotlin.jvm.functions.Function2");
			Class<?> lintErrorClass = classLoader.loadClass("com.github.shyiko.ktlint.core.LintError");
			Method detailGetter = lintErrorClass.getMethod("getDetail");
			Method lineGetter = lintErrorClass.getMethod("getLine");
			Method colGetter = lintErrorClass.getMethod("getCol");
			Object formatterCallback = Proxy.newProxyInstance(classLoader, new Class[]{function2Interface},
					(proxy, method, args) -> {
						Object lintError = args[0]; // com.github.shyiko.ktlint.core.LintError
						boolean corrected = (Boolean) args[1];
						if (!corrected) {
							String detail = (String) detailGetter.invoke(lintError);
							int line = (Integer) lineGetter.invoke(lintError);
							int col = (Integer) colGetter.invoke(lintError);
							throw new AssertionError("Error on line: " + line + ", column: " + col + "\n" + detail);
						}
						return null;
					});

			// grab the KtLint singleton
			Class<?> ktlintClass = classLoader.loadClass("com.github.shyiko.ktlint.core.KtLint");
			Object ktlint = ktlintClass.getDeclaredField("INSTANCE").get(null);
			// and its format method
			String formatterMethodName = isScript ? "formatScript" : "format";
			Method formatterMethod = ktlintClass.getMethod(formatterMethodName, String.class, Iterable.class, Map.class, function2Interface);

			return input -> {
				try {
					return (String) formatterMethod.invoke(ktlint, input, ruleSets, userData, formatterCallback);
				} catch (InvocationTargetException e) {
					throw ThrowingEx.unwrapCause(e);
				}
			};*/
		}
	}
}
