package org.fao.unredd.portal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.sf.json.JSONObject;

public class RoleConfigurationProviderTest {
	private File configDir, roleDir;
	private RoleConfigurationProvider provider;
	private PluginDescriptor plugin;

	@Before
	public void setup() throws IOException {
		configDir = File.createTempFile("geoladris", "");
		configDir.delete();
		configDir.mkdir();

		roleDir = new File(configDir, RoleConfigurationProvider.ROLE_DIR);
		roleDir.mkdir();

		plugin = new PluginDescriptor(true);
		plugin.setName("myplugin");

		Map<String, PluginDescriptor> plugins = new HashMap<>();
		plugins.put(plugin.getName(), plugin);

		provider = new RoleConfigurationProvider(configDir, plugins);
	}

	@After
	public void teardown() throws IOException {
		FileUtils.deleteDirectory(configDir);
	}

	@Test
	public void noRoleOnRequest() throws Exception {
		HttpServletRequest request = mockRequest(null);
		Map<PluginDescriptor, JSONObject> conf = provider.getPluginConfig(
				mock(PortalRequestConfiguration.class), request);
		assertNull(conf);
	}

	@Test
	public void roleWithoutSpecificConf() throws Exception {
		HttpServletRequest request = mockRequest("role1");
		Map<PluginDescriptor, JSONObject> conf = provider.getPluginConfig(
				mock(PortalRequestConfiguration.class), request);
		assertNull(conf);
	}

	@Test
	public void addsPlugin() throws Exception {
		String role = "role1";

		File tmp = new File(roleDir, role + ".json");
		FileWriter writer = new FileWriter(tmp);
		IOUtils.write(
				"{ '" + plugin.getName() + "' : { mymodule : {'a' : true }}}",
				writer);
		writer.close();

		HttpServletRequest request = mockRequest(role);
		Map<PluginDescriptor, JSONObject> pluginConfs = provider
				.getPluginConfig(mock(PortalRequestConfiguration.class),
						request);
		assertEquals(1, pluginConfs.size());
		assertTrue(pluginConfs.containsKey(plugin));
		JSONObject pluginConf = pluginConfs.get(plugin);
		assertTrue(pluginConf.getJSONObject("mymodule").getBoolean("a"));

		tmp.delete();
	}

	@Test
	public void cannnotBeCached() {
		assertFalse(this.provider.canBeCached());
	}

	private HttpServletRequest mockRequest(String role) {
		HttpServletRequest request = mock(HttpServletRequest.class);

		HttpSession session = mock(HttpSession.class);
		when(request.getSession()).thenReturn(session);
		when(session.getAttribute(Constants.SESSION_ATTR_ROLE))
				.thenReturn(role);

		return request;
	}
}
