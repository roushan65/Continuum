import React from 'react';
import * as Native from 'react-native';

export default class HomePage extends React.Component {
  properties = null;

  constructor(props) {
    super(props);
    this.properties = props
    console.log('props: ', props);
    this.state = {
      message: 'Loading Movelet from remote...',
      refreshing: false
    };
  }

  componentDidMount() {
    console.log('HomePage mounted!');
  }

  onRefresh=()=>{
    this.setState({
      refreshing:true
    });
    this.properties.onRefresh()
    this.setState({
      refreshing:false
    });
  }

  render() {
    return (
      <Native.SafeAreaView style={styles.container}>
        <Native.ScrollView
          contentContainerStyle={styles.scrollView}
          refreshControl={
            <Native.RefreshControl
              refreshing={this.state.refreshing}
              onRefresh={this.onRefresh}
            />
          }>
          <Native.Text>{this.state.message}</Native.Text>
        </Native.ScrollView>
      </Native.SafeAreaView>
    );
  }
}


const styles = Native.StyleSheet.create({
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