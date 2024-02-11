#!groovy

def decidePipeline(Map configMap){
    application = configMAP.get("application")
    //herew we are getting nodeJSVM
    switch (application){
        case 'nodeJSVM':
            echo "application is node JS and VM based"
            // nodeJSVMCI(configMap)
            break
        case 'JavaVM':
            javaVMCI(configMap)
            break
        default:
            error "Un crecognised application"
    }
}

