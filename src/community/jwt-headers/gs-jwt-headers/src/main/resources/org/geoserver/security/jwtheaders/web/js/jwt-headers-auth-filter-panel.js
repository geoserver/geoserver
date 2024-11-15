// When the user name format dropdown is changed, we hide
// the "json path" input if its not needed.
function usernameFormatChanged() {
    var elem = document.getElementById("userNameFormatSelect")
    if (elem.value === "JSON" || elem.value === "JWT") {
        document.getElementById("userNamePathDiv").style.display = "block";
    } else {
        document.getElementById("userNamePathDiv").style.display = "none";
    }
}

function showTokenValidationChanged() {
    var elem = document.getElementById("validateToken")
    var div = document.getElementById("validateTokenDiv");
    if (elem.checked) {
        div.style.display = "block";
    } else {
        div.style.display = "none";
    }
}

function toggleVisible(element,id) {
    var visibilityDiv = document.getElementById(id);
    if (element.checked)
        visibilityDiv.style.display = "block";
    else
        visibilityDiv.style.display = "none";
}

function reset() {
    usernameFormatChanged();
    showTokenValidationChanged();
    toggleVisible(document.getElementById('validateTokenSignature'),'validateTokenSignatureURLDiv');
    toggleVisible(document.getElementById('validateTokenAgainstURL'),'validateTokenAgainstURLDiv');
    toggleVisible(document.getElementById('validateTokenAudience'),'validateTokenAudienceDiv');
}


//takes the converter string (in "att1=val1;att2=val2" format)
//and adds rows to the roleConverterStringTbody.
//If there aren't any items in the converter string, then
//hide the table.
function roleConverterStringChanged() {
    //actual (string) value of the att1=val1;att2=val2 map
    var strElement = document.getElementById("roleConverterString");
    if (strElement == null) {
        //loading
        return;
    }
    var str = strElement.value;

    //body of the table - under the <th>s
    var tbody = document.getElementById("roleConverterStringTbody");

    //div containing the table (and title).
    //we only display it if there are conversions
    var div = document.getElementById("roleConverterStringDiv");

    if (str == null) {
        div.style.display = "none";
        return; //nothing to do
    }
    str = str.trim();
    if (str === "") {
        div.style.display = "none";
        return; //nothing to do
    }

    //one for each of the items in the Map
    var items = str.split(";");
    var body = "";
    for (var i = 0; i < items.length; i++) {
        //parts[0]=key, parts[1]=value
        var parts = items[i].split("=");
        if (parts.length != 2) {
            continue; // invalid
        }
        var externalRole = removeBadChars(parts[0]);
        var gsRole = removeBadChars(parts[1]);
        var tr = "<tr><td>" + externalRole + "</td><td>" + gsRole + "</td></tr>";
        body += tr;
    }
    tbody.innerHTML = body;
    div.style.display = "block";
}

//we don't want to put user input into the dom, so we
//make sure there isn't anything "bad" (like "script") in the
//input
function removeBadChars(inputString) {
    var regex = new RegExp('[^0-9a-zA-Z_.\-]', 'g');
    return inputString.replace(regex, '');
}