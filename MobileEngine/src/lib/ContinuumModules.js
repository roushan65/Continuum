const modules = {
    'react': require('react'),
    'react-native': require('react-native'),
    '@react-navigation/native': require('@react-navigation/native'),
    '@react-navigation/native-stack': require('@react-navigation/native-stack'),
    'react/jsx-runtime': require('react/jsx-runtime')
    // 'expo-app-loading': require('expo-app-loading'),
    // '@use-expo/font': require('@use-expo/font'),
    // 'expo-asset': require('expo-asset'),
    // 'galio-framework': require('galio-framework'),
    // '@react-navigation/bottom-tabs': require('@react-navigation/bottom-tabs'),
    // '@react-navigation/drawer': require('@react-navigation/drawer'),
    // '@react-navigation/stack': require('@react-navigation/stack'),
    // 'prop-types': require('prop-types'),
    // 'react-native-reanimated': require('react-native-reanimated'),
    // 'react-native-modal-dropdown': require('react-native-modal-dropdown'),
    // 'react-native-gesture-handler': require('react-native-gesture-handler'),
    // 'expo-font': require('expo-font'),
    // '@react-navigation/compat': require('@react-navigation/compat'),
    // 'react-native-screens': require('react-native-screens')
};

export default class ContinuumModules {

    require = (module) => {
        if(modules[module]) {
            console.log("loading module", module);
            return modules[module]
        } else {
            throw `module '${module}' not found`
        }
    }
    
}