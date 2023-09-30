const React = require('react')
const {View, Text, Button} = require('react-native')

export default class ProfilePage extends React.Component {
    constructor(props) {
      super(props);
    }

    onPress = ()=>{
      console.log("You pressed me!");
    }
  
    render = ()=>{
      return (
        <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
          <Text>Profile Page</Text>
          <Button onPress={this.onPress} title="Press Me" color="green" />
        </View>
      );
    }
}