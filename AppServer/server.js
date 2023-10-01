const app = require("express")();
const Metro = require("metro");
const fs = require('fs')

function resolver(context, moduleName, platform) {
  if (fs.existsSync(`${__dirname}/packages/${moduleName}`)) {
    // console.log(`resolving from contimuum ${moduleName}, ${platform}`);
    return {
      filePath: `${__dirname}/packages/${moduleName}/index.js`,
      type: "sourceFile",
    };
  }
  return context.resolveRequest(context, moduleName, platform);
}

// We first load the config from the file system
Metro.loadConfig().then(async (config) => {
  // config.resolver.resolveRequest = resolver;
  const metroBundlerServer = await Metro.runMetro(config);

  app.use(metroBundlerServer.processRequest.bind(metroBundlerServer));

  app.listen(9091);
});
