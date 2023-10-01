const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');
const fs = require('fs');
/**
 * Metro configuration
 * https://facebook.github.io/metro/docs/configuration
 *
 * @type {import('metro-config').MetroConfig}
 */
const config = {
    resolver: {
        resolveRequest: (context, moduleName, platform) => {
            if (fs.existsSync(`${__dirname}/packages/${moduleName}`)) {
              // console.log(`resolving from contimuum ${moduleName}, ${platform}`);
              return {
                filePath: `${__dirname}/packages/${moduleName}/index.js`,
                type: "sourceFile",
              };
            }
            return context.resolveRequest(context, moduleName, platform);
        }
    },
    serializer: {
      getModulesRunBeforeMainModule: () => []
    }
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);
