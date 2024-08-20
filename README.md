# Jenkins ConfigOps Plugin

## 介绍

配合 [ConfigOps](https://github.com/dumasd/config-ops) 执行配置变更。

## 开始使用

### ConfigOps 部署

在 Jenkins Agent 机器上部署 ConfigOps 服务，文档参考：[config-ops/README.md](https://github.com/dumasd/config-ops)

### Nacos 配置变更

Nacos 配置的目录结构需要按照下面的模版存储，存储形式可以是Git、制品库等。

```
Base Dir/                       # Base Directory
    namespace-id1/                     # Nacos Namespace ID
        group1/                       # Nacos Group
            v0.0.1/                     # DataID 增量配置文件夹
                config.properties  
                config.yaml
            v0.0.2/
                config.yaml           
            config.properties           # DataID 全量配置
            config.yaml
        group2/
            v0.0.1/
                messages.properties
                config.yaml
            v0.0.2/
                config.yaml  
            messages.properties
            config.yaml
    namespace-id2/
        group3/
            v1.0.0/
                messages.properties
                config.yaml
            v1.0.1/
                config.yaml          
            application.properties
            application.yaml
        group4/
            v1.0.0/
                application.properties
                application.yaml
            v1.0.1/
                application.yaml          
            application.properties
            application.yaml
```
1. nacosConfigGet (Jenkins Step) 读取Nacos配置文件

``` groovy
def nacosFiles = nacosConfigGet(workingDir: 'script/nacos')
```

2. nacosConfigChoices (Jenkins Parameter) 选择需要执行的Nacos配置

```groovy
// 弹出界面,选择要更改的nacos配置
def choiceResult = input(message: 'Choice', parameters: [nacosConfigChoices(choices: nacosFiles)])
```

3. nacosConfigModifyPreview (Jenkins Step) Nacos配置修改预览

```
// 获取修改预览
def previewResult = nacosConfigModifyPreview(workingDir: 'script/nacos', nacosId: "${NACOS_ID}", toolUrl: "${CONFIG_OPS_URL}", items: choiceResult['values'])
```

4. nacosConfigEdit (Jenkins Parameter) Nacos配置修改预览界面，配置可进一步修改

```groovy
// 弹出界面，预览每个DataId修改前后的对比情况，还可以根据情况进一步修改
def editResult = input(message: 'Preview Edit', parameters: [nacosConfigEdit(items: previewResult['values'])])
```

5. nacosConfigModifyApply (Jenkins Step) Nacos配置修改应用

``` groovy
nacosConfigModifyApply(nacosId: "${NACOS_ID}", toolUrl: "${CONFIG_OPS_URL}", items: editResult['values'])
```

完整示例

``` groovy
import groovy.json.JsonSlurper

pipeline {
    agent any
    
    environment {
        CONFIG_OPS_URL = 'http://127.0.0.1:5000'
        gitUrl = 'https://github.com/thinkerwolf/jenkins-example.git'
        gitCredential = 'git-cred'
        NACOS_ID = 'default'
    }
    
    stages {
        stage('SCM checkout') {
            steps {
                checkout scmGit(
                        branches: [[name: "*/main"]],
                        extensions: [cloneOption(shallow: true)],
                        userRemoteConfigs: [[credentialsId: gitCredential, url: gitUrl]]
                )
            }
        }
       
        stage('Execute') {
            steps {
                script {
                    def jsonOutput = groovy.json.JsonOutput
                    
                    // 浏览制品库nacos配置文件
                    def nacosFiles = nacosConfigGet(workingDir: 'script/nacos')
                    def nacosFilesJson = jsonOutput.toJson(nacosFiles)
                    echo "${nacosFilesJson}"
                    
                    // 弹出界面选择要更改的nacos配置
                    def choiceResult = input(message: 'Choice', parameters: [nacosConfigChoices(choices: nacosFiles)])
                    def choiceResultJson = jsonOutput.toJson(choiceResult)
                    echo "${choiceResultJson}"
                    
                    if (choiceResult['showPreview']) {
                        // 获取修改预览
                        def previewResult = nacosConfigModifyPreview(workingDir: 'script/nacos', nacosId: "${NACOS_ID}", toolUrl: "${CONFIG_OPS_URL}", items: choiceResult['values'])
                        
                        // 弹出预览界面
                        def editResult = input(message: 'Preview Edit', parameters: [nacosConfigEdit(items: previewResult['values'])])
                        
                        // 修改配置
                        nacosConfigModifyApply(nacosId: "${NACOS_ID}", toolUrl: "${CONFIG_OPS_URL}", items: editResult['values'])
                    } else {
                        // 获取修改预览
                        def previewResult = nacosConfigModifyPreview(workingDir: 'script/nacos', nacosId: "${NACOS_ID}", toolUrl: "${CONFIG_OPS_URL}", items: choiceResult['values'])
                        
                        // 修改配置
                        nacosConfigModifyApply(nacosId: "${NACOS_ID}", toolUrl: "${CONFIG_OPS_URL}", items: previewResult['values'])
                    }
                    
                }
            }
        }
    }
}

```


### 数据库脚本执行

数据库SQL脚本的目录结构需要按照下面的模版存储，存储形式可以是Git、制品库等。
```
Base Dir/                 # Base Directory
    database_name1/         # Database Name      
        sql_v0.0.1.sql        # sql file
        sql_v0.0.2.sql
    database_name3/               
        sql_v0.0.1.sql        
        sql_v0.0.2.sql    
```

1. databaseConfigGet (Jenkins Step) 读取数据库脚本文件

```groovy
def files = databaseConfigGet(workingDir: "${WORKING_DIR}")
```

2. databaseConfigChoices (Jenkins Parameter) 选择需要执行的数据脚本

```groovy
def choiceResult = input(message: 'Choice', parameters: [databaseConfigChoices(choices: files)])
```

3. databaseConfigApply (Jenkins Step) 执行数据库脚本

```groovy
databaseConfigApply(workingDir: "${WORKING_DIR}", databaseId: "${DATABASE_ID}", toolUrl: "${CONFIG_OPS_URL}", items: choiceResult['values'])
```

完整示例

```groovy
pipeline {
    agent any
    
    environment {
        CONFIG_OPS_URL = 'http://127.0.0.1:5000'
        gitUrl = 'https://github.com/thinkerwolf/jenkins-example.git'
        gitCredential = 'git-cred'
        DATABASE_ID = 'vod-dev'
        WORKING_DIR = 'script/mysql'
    }
    
    stages {
        stage('SCM checkout') {
            steps {
                checkout scmGit(
                        branches: [[name: "*/main"]],
                        extensions: [cloneOption(shallow: true)],
                        userRemoteConfigs: [[credentialsId: gitCredential, url: gitUrl]]
                )
            }
        }
        
        stage('Execute') {
            steps {
                script {
                    def files = databaseConfigGet(workingDir: "${WORKING_DIR}", maxFileNum: 50)
                    def choiceResult = input(message: 'Choice', parameters: [databaseConfigChoices(choices: files)])
                    databaseConfigApply(workingDir: "${WORKING_DIR}", databaseId: "${DATABASE_ID}", toolUrl: "${CONFIG_OPS_URL}", items: choiceResult['values'])
                }
            }
        }
    }
}

```


## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)

