package org.fao.unredd.portal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;
import org.junit.Test;

import net.sf.json.JSONObject;

public class DefaultConfProviderTest {
	@Test
	public void returnsDefaultConfFromPlugins() throws Exception {
		String pluginConf1 = "{module1 : {prop1 : 42, prop2 : true}}";
		String pluginConf2 = "{module2 : {prop3 : 'test'},"
				+ "module3 : [4, 2, 9]}";

		Set<PluginDescriptor> plugins = new HashSet<>();
		PluginDescriptor p1 = mockPlugin("myplugin1", pluginConf1);
		PluginDescriptor p2 = mockPlugin("myplugin2", pluginConf2);
		plugins.add(p1);
		plugins.add(p2);

		DefaultConfProvider provider = new DefaultConfProvider(plugins);
		Map<PluginDescriptor, JSONObject> config = provider.getPluginConfig(
				mock(PortalRequestConfiguration.class),
				mock(HttpServletRequest.class));

		assertEquals(2, config.size());
		JSONObject conf1 = config.get(p1);
		JSONObject conf2 = config.get(p2);
		assertEquals(42, conf1.getJSONObject("module1").getInt("prop1"));
		assertTrue(conf1.getJSONObject("module1").getBoolean("prop2"));
		assertEquals("test", conf2.getJSONObject("module2").getString("prop3"));
		assertEquals(4, conf2.getJSONArray("module3").get(0));
		assertEquals(2, conf2.getJSONArray("module3").get(1));
		assertEquals(9, conf2.getJSONArray("module3").get(2));
	}

	@Test
	public void canBeCached() {
		DefaultConfProvider provider = new DefaultConfProvider(
				new HashSet<PluginDescriptor>());
		assertTrue(provider.canBeCached());
	}

	private PluginDescriptor mockPlugin(String name, String defaultConf) {
		PluginDescriptor plugin = new PluginDescriptor(true);
		plugin.setName(name);
		plugin.setConfiguration("{'default-conf' : " + defaultConf + "}");
		return plugin;
	}
}