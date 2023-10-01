import React from 'react';
import HomePage from './components/HomePage';
import ContinuumModules from './lib/ContinuumModules';
import { AppRegistry, Platform } from 'react-native';

export default class App extends React.Component {
  jsxServerURL = "http://localhost:9091/applets/app1/index.bundle?platform=ios&dev=true&minify=false&modulesOnly=false&runModule=true&app=org.reactjs.native.example.Continuum";

  constructor(props) {
    super(props);
    this.movilizer={exports:null};
    this.state = {
      jsx: "",
      refreshing:false
    }
    if(Platform.OS === 'android') {
      this.jsxServerURL = this.jsxServerURL.replace("localhost", "10.0.2.2");
    }
  }

  onRefresh=()=>{
    console.log("refereshing from \""+this.jsxServerURL+"\" ...");
    fetch(this.jsxServerURL,{
      method:"GET",
      headers:{
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        'Pragma': 'no-cache',
        'Expires': 0
      }
    }).then((resp)=>{
      return resp.text()
    }).then((text)=>{
      console.log(text);
      this.setState({
        jsx:text
      });
    }).catch((e)=>{
      console.log("Error: " + e );
    });
  }

  // onRefresh = ()=>{
  //   NativeModules.BundleLoader.load("http://localhost:8081/applets/app1/index.bundle?platform=ios&dev=true&minify=false&modulesOnly=false&runModule=true&app=org.reactjs.native.example.Continuum")
  //   // BundleLoader.load("http://localhost:8081/applets/app1/index.bundle?platform=ios&dev=true&minify=false&modulesOnly=false&runModule=true&app=org.reactjs.native.example.Continuum")
  // }

  componentDidMount(){
    // this.onRefresh();
  }

  render(){
    let code = this.state.jsx; //No need to transpile the code as it is already transpile from AppServer
    if(code != "") {
      let App = Function("Continuum", code);
      let contimuumModules = new ContinuumModules();
      let Continuum = {require: contimuumModules.require, registerComponen: AppRegistry.registerComponent}
      App(Continuum);
      Applet = Continuum.appRoot
      console.log("approot= ", Applet);
      return <Applet/>
    }
    return <HomePage onRefresh={this.onRefresh}/>;
  }
}
