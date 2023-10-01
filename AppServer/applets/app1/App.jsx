import HomePage from './pages/HomePage/HomePage';
// const HomePage = require('./pages/HomePage/HomePage')
const ProfilePage = require('./pages/ProfilePage/ProfilePage')

const React = require('react')
const {StyleSheet} = require('react-native')
const {NavigationContainer} = require('@react-navigation/native')
const {createNativeStackNavigator} = require('@react-navigation/native-stack')

const Stack = createNativeStackNavigator();

module.exports = class App extends React.Component {

  constructor(props) {
    super(props);
  }

  componentDidMount(){
    console.log("Applet mounted!")
  }

  onOkPressed() {
    console.log("Ok Pressed!")
  }

  render(){
    return (
      <NavigationContainer>
        <Stack.Navigator>
          <Stack.Screen name="Home">
            {(props) => <HomePage {...props} extraData={"extraData"}/>}
          </Stack.Screen>
          <Stack.Screen name="Profile" component={ProfilePage} />
        </Stack.Navigator>
      </NavigationContainer>
    );
  }
  
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    marginTop: 0,
  },
  scrollView: {
    flex: 1,
    backgroundColor: 'white',
    alignItems: 'center',
  },
});
