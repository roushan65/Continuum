const React = require("react");
const {View, Text, Button, StyleSheet} = require("react-native");

module.exports = class HomePage extends React.Component {

  constructor(props) {
    super(props);
    console.log(props);
    this.navigation = props.navigation;
  }

  onPress = () => {
    console.log("You pressed me!");
  }

  onProfile = () => {
    this.navigation.navigate("Profile");
  }

  render = () => {
    return (
      <View style={{ flex: 1, alignItems: "center", justifyContent: "center" }}>
        <Text>Home Page</Text>
        <Button onPress={this.onPress} title="Press Me" color="green" />
        <Button onPress={this.onProfile} title="Profile" color="blue" />
      </View>
    );
  };
  
}
