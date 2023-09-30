const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');
const fs = require('fs')
/**
 * Metro configuration
 * https://facebook.github.io/metro/docs/configuration
 *
 * @type {import('metro-config').MetroConfig}
 */
const config = {
    server: {
        port: 3000
    },
    resolver: {
        resolveRequest: (context, moduleName, platform) => {
            if(fs.existsSync(`${__dirname}/packages/${moduleName}`)) {
                console.log(`resolving from contimuum ${moduleName}, ${platform}`)
                return {
                    filePath: `${__dirname}/packages/${moduleName}/index.js`,
                    type: 'sourceFile',
                };
            }
            return context.resolveRequest(context, moduleName, platform)
        }
    }
};
console.log(getDefaultConfig(__dirname))
module.exports = mergeConfig(getDefaultConfig(__dirname), config);
