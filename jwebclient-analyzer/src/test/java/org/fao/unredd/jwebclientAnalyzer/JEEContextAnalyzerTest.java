package org.fao.unredd.jwebclientAnalyzer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.input.BoundedInputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class JEEContextAnalyzerTest {
	private static File test1LibFolder = new File(
			"src/test/resources/test1/WEB-INF/lib");
	private static File testOnlyLibFolder = new File(
			"src/test/resources/testOnlyLib/WEB-INF/lib");

	@BeforeClass
	public static void packAsJar() throws IOException {
		packageAsJar("test2", ".", test1LibFolder);

		packageAsJar("test2", ".", testOnlyLibFolder);
		packageAsJar("testJavaNonRootModules", "WEB-INF/classes",
				testOnlyLibFolder);
	}

	private static void packageAsJar(String testCaseToPack,
			String pluginContentsRoot, File jslib)
			throws FileNotFoundException, IOException {
		assertTrue(jslib.exists() || jslib.mkdirs());

		File jarFile = new File(jslib, testCaseToPack + ".jar");
		assertTrue(!jarFile.exists() || jarFile.delete());

		FileOutputStream stream = new FileOutputStream(jarFile);
		File jarContentRoot = new File("src/test/resources/", testCaseToPack
				+ File.separator + pluginContentsRoot);
		Collection<File> files = FileUtils.listFiles(jarContentRoot,
				TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
		JarOutputStream out = new JarOutputStream(stream);
		for (File file : files) {
			String entryName = file.getPath();
			entryName = entryName
					.substring(jarContentRoot.getPath().length() + 1);
			out.putNextEntry(new ZipEntry(entryName));
			InputStream entryInputStream = new BoundedInputStream(
					new FileInputStream(file));
			IOUtils.copy(entryInputStream, out);
			entryInputStream.close();
		}
		out.close();
	}

	@AfterClass
	public static void removeTest2Jar() throws IOException {
		FileUtils.deleteDirectory(test1LibFolder);
		FileUtils.deleteDirectory(testOnlyLibFolder.getParentFile());
	}

	@Test
	public void checkTest1() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(new FileContext(
				"src/test/resources/test1"));

		checkList(context.getRequireJSModuleNames(), "module1", "module2",
				"module3");
		checkList(context.getCSSRelativePaths(), "styles/general.css",
				"modules/module2.css", "modules/module3.css",
				"styles/general2.css");
		checkMapKeys(context.getNonRequirePathMap(), "jquery-ui", "fancy-box",
				"openlayers", "mustache");
		checkMapKeys(context.getNonRequireShimMap(), "fancy-box", "mustache");
		Map<String, JSONObject> confElements = context
				.getConfigurationElements();
		assertEquals("29px", confElements.get("layout")
				.getString("banner-size"));
		assertEquals(true, confElements.get("legend").getBoolean("show-title"));
		assertEquals(14, confElements.get("layer-list").getInt("top"));
	}

	@Test
	public void checkExpandedClient() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(
				new ExpandedClientContext("src/test/resources/test2"));

		checkList(context.getRequireJSModuleNames(), "module3");
		checkList(context.getCSSRelativePaths(), "modules/module3.css",
				"styles/general2.css");
		checkMapKeys(context.getNonRequirePathMap(), "mustache");
		checkMapKeys(context.getNonRequireShimMap(), "mustache");
	}

	@Test
	public void checkCustomPluginConfDir() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(new FileContext(
				"src/test/resources/test3"), "conf", "webapp");

		checkMapKeys(context.getNonRequirePathMap(), "jquery-ui", "fancy-box",
				"openlayers");
		checkMapKeys(context.getNonRequireShimMap(), "fancy-box");
	}

	@Test
	public void checkCustomWebResourcesDir() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(new FileContext(
				"src/test/resources/test3"), "conf", "webapp");

		checkList(context.getRequireJSModuleNames(), "module1", "module2");
		checkList(context.getCSSRelativePaths(), "styles/general.css",
				"modules/module2.css");
	}

	@Test
	public void scanNoJavaPlugins() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(new FileContext(
				"src/test/resources/testNoJava"), "nfms", "nfms");
		checkList(context.getRequireJSModuleNames(), //
				"j/module-java",//
				"plugin1/a",//
				"plugin1/b",//
				"plugin2/c");
		checkList(context.getCSSRelativePaths(),//
				"styles/j/style-java.css",//
				"styles/plugin1/a.css",//
				"styles/plugin2/b.css",//
				"styles/plugin2/c.css",//
				"modules/plugin1/d.css",//
				"modules/j/module-style-java.css");

		Map<String, String> nonRequirePaths = context.getNonRequirePathMap();
		assertEquals("../jslib/plugin1/lib-a", nonRequirePaths.get("lib-a"));
		assertEquals("../jslib/plugin1/lib-b", nonRequirePaths.get("lib-b"));
		assertEquals("../jslib/plugin2/lib-c", nonRequirePaths.get("lib-c"));
		assertEquals("../jslib/j/lib-java1", nonRequirePaths.get("lib-java1"));
		assertEquals("../jslib/j/lib-java2", nonRequirePaths.get("lib-java2"));
		assertEquals(5, nonRequirePaths.size());

		Map<String, String> nonRequireShims = context.getNonRequireShimMap();
		assertEquals("[\"lib-a\",\"lib-b\",\"lib-c\"]",
				nonRequireShims.get("lib-java"));
		assertEquals("[\"lib-a\"]", nonRequireShims.get("lib-b"));
		assertEquals("[\"lib-a\",\"lib-b\"]", nonRequireShims.get("lib-c"));
		assertEquals(3, nonRequireShims.size());
	}

	@Test
	public void scanJavaNonRootModules() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(new FileContext(
				"src/test/resources/testJavaNonRootModules"), "nfms", "nfms");
		checkList(context.getRequireJSModuleNames(), "j/module1");
		checkList(context.getCSSRelativePaths(), "styles/j/style1.css",
				"modules/j/module1.css");
		Map<String, String> nonRequirePaths = context.getNonRequirePathMap();
		assertEquals("../jslib/j/lib", nonRequirePaths.get("lib"));
		assertEquals(1, nonRequirePaths.size());
	}

	@Test
	public void scanJavaNonRootModulesAsJar() {
		JEEContextAnalyzer context = new JEEContextAnalyzer(new FileContext(
				"src/test/resources/testOnlyLib"), "nfms", "nfms");
		checkList(context.getRequireJSModuleNames(),//
				"j/module1",//
				"module3"//
		);
		checkList(context.getCSSRelativePaths(), //
				"styles/j/style1.css",//
				"modules/j/module1.css",//
				"styles/general2.css",//
				"modules/module3.css"//
		);
		Map<String, String> nonRequirePaths = context.getNonRequirePathMap();
		assertEquals("../jslib/j/lib", nonRequirePaths.get("lib"));
		assertEquals("../jslib/jquery.mustache",
				nonRequirePaths.get("mustache"));
		assertEquals(2, nonRequirePaths.size());
	}

	private void checkList(List<String> result, String... testEntries) {
		for (String entry : testEntries) {
			assertTrue(entry + " not in " + result, result.remove(entry));
		}

		assertTrue(result.size() == 0);
	}

	private void checkMapKeys(Map<String, ?> result, String... testKeys) {
		for (String entry : testKeys) {
			assertTrue(result.remove(entry) != null);
		}

		assertTrue(result.size() == 0);
	}
}
