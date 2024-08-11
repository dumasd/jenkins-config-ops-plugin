/**
 *
 * @param select
 * @param url
 * @param params
 * @param success
 * @param failure
 */
function requestAndFillNacosOptions(select, url, params, success, failure) {
    select.innerHTML = ''
    new Ajax.Request(url, {
        method: 'post',
        parameters: params,
        onSuccess: function (response) {
            const json = response.responseText.evalJSON();
            if (!json.values || json.values.length === 0) {
                failure()
                return;
            }
            const options = json.values
            for (let i = 0; i < options.length; i++) {
                let subOption = options[i]
                let optionElement = document.createElement('option')
                optionElement.value = subOption.value
                optionElement.textContent = subOption.name
                optionElement.selected = subOption.selected
                select.appendChild(optionElement)
            }
            success()
        },
        onFailure: function (response) {
            failure()
            alert('Request failed: ' + response.statusText);
        }
    })
}


/**
 *
 * @param ngSel
 * @param dataIdSel
 * @param verSel
 * @param baseUrl
 * @param json
 */
function updateDataIdItems(ngSel, dataIdSel, verSel, baseUrl, json) {
    let index = ngSel.selectedIndex
    let namespaceGroup = ngSel.options[index].value
    requestAndFillNacosOptions(dataIdSel, baseUrl + "/fillDataIdItems", {
        json: json,
        namespaceGroup: namespaceGroup
    }, () => {
        updateVersionItems(ngSel, dataIdSel, verSel, baseUrl, json)
    }, () => {
        dataIdSel.disabled = false
        ngSel.disabled = false
        dataIdSel.disabled = false
    })
}

/**
 *
 * @param ngSel
 * @param dataIdSel
 * @param verSel
 * @param baseUrl
 * @param json
 */
function updateVersionItems(ngSel, dataIdSel, verSel, baseUrl, json) {
    let index = ngSel.selectedIndex
    let namespaceGroup = ngSel.options[index].value
    index = dataIdSel.selectedIndex
    let dataId = dataIdSel.options[index].value
    requestAndFillNacosOptions(verSel, baseUrl + "/fillVersionItems", {
        json: json,
        namespaceGroup: namespaceGroup,
        dataId: dataId
    }, () => {
        verSel.disabled = false
        ngSel.disabled = false
        dataIdSel.disabled = false
    }, () => {
        verSel.disabled = false
        ngSel.disabled = false
        dataIdSel.disabled = false
    })
}
