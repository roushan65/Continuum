let express = require('express');
let app = express();
let browserify = require("browserify");
let path = require("path");

app.get('/*', function (req, res) {
    filePath = req.params[0];
    if(filePath.endsWith(".js") || filePath.endsWith(".jsx")){
        console.log("Building "+filePath+" ...");
        req.query.dev = req.query.dev == "true";
        browserify(filePath,{debug: req.query.dev})
            .transform("babelify", { 
                presets: ["@babel/preset-env", "@babel/preset-react"],
                extensions: [".jsx" ,".js"],
                sourceFileName: filePath
            }).bundle().pipe(res);
        console.log("Done!!");
    }else{
        res.sendFile(path.join(__dirname, filePath));
    }
});

app.listen(3000);