package org.fao.unredd;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;
import org.fao.unredd.jwebclientAnalyzer.Context;
import org.fao.unredd.jwebclientAnalyzer.JEEContextAnalyzer;
import org.fao.unredd.jwebclientAnalyzer.PluginDescriptor;
import org.fao.unredd.portal.ConfigFolder;
import org.fao.unredd.portal.DefaultConfProvider;
import org.fao.unredd.portal.DefaultConfig;
import org.fao.unredd.portal.ModuleConfigurationProvider;
import org.fao.unredd.portal.PluginJSONConfigurationProvider;
import org.fao.unredd.portal.PublicConfProvider;
import org.fao.unredd.portal.RoleConfigurationProvider;

public class AppContextListener implements ServletContextListener {
	private static final Logger logger = Logger
			.getLogger(AppContextListener.class);

	public static final String ENV_CONFIG_CACHE = "NFMS_CONFIG_CACHE";
	public static final String INIT_PARAM_DIR = "PROTAL_CONFIG_DIR";

	public static final String ATTR_CONFIG = "config";

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		contextInitialized(sce.getServletContext());
	}

	void contextInitialized(ServletContext servletContext) {

		String rootPath = servletContext.getRealPath("/");
		String configInitParameter = servletContext
				.getInitParameter(INIT_PARAM_DIR);
		boolean configCache = Boolean
				.parseBoolean(System.getenv(ENV_CONFIG_CACHE));
		ConfigFolder folder = new ConfigFolder(rootPath, configInitParameter);

		JEEContext context = new JEEContext(servletContext,
				new File(folder.getFilePath(), "plugins"));
		JEEContextAnalyzer analyzer = getAnalyzer(context);

		Set<PluginDescriptor> plugins = analyzer.getPluginDescriptors();
		Map<String, PluginDescriptor> pluginNameMap = new HashMap<>();
		for (PluginDescriptor plugin : plugins) {
			if (plugin.getName() != null) {
				pluginNameMap.put(plugin.getName(), plugin);
			}
		}

		File publicConf = new File(folder.getFilePath(),
				PublicConfProvider.FILE);
		boolean hasPublicConf = publicConf.exists() && publicConf.isFile();
		ModuleConfigurationProvider confProvider;
		if (hasPublicConf) {
			confProvider = new PublicConfProvider(folder.getFilePath(),
					pluginNameMap);
		} else {
			confProvider = new PluginJSONConfigurationProvider();
			logger.warn("plugin-conf.json file for configuration has been "
					+ "deprecated. Use public-conf.json instead.");

		}

		DefaultConfig config = new DefaultConfig(folder, plugins,
				new DefaultConfProvider(plugins), configCache);
		config.addModuleConfigurationProvider(confProvider);
		config.addModuleConfigurationProvider(new RoleConfigurationProvider(
				folder.getFilePath(), pluginNameMap));

		servletContext.setAttribute(ATTR_CONFIG, config);
	}

	/**
	 * For testing purposes.
	 */
	JEEContextAnalyzer getAnalyzer(Context context) {
		return new JEEContextAnalyzer(context);
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

	private class JEEContext implements Context {

		private ServletContext servletContext;
		private File noJavaRoot;

		public JEEContext(ServletContext servletContext, File noJavaRoot) {
			this.servletContext = servletContext;
			this.noJavaRoot = noJavaRoot;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Set<String> getLibPaths() {
			return servletContext.getResourcePaths("/WEB-INF/lib");
		}

		@Override
		public InputStream getLibAsStream(String jarFileName) {
			return servletContext.getResourceAsStream(jarFileName);
		}

		@Override
		public File getClientRoot() {
			return new File(servletContext.getRealPath("/WEB-INF/classes/"));
		}

		@Override
		public File getNoJavaRoot() {
			return noJavaRoot;
		}
	}
}