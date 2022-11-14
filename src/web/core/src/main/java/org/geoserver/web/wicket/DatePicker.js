/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

// below some variable and functions to map date format tokens from java to moment js eg. d -> D for day of month.
var javaFormatMapping = {
    d: 'D',
    dd: 'DD',
    y: 'YYYY',
    yy: 'YY',
    yyy: 'YYYY',
    yyyy: 'YYYY',
    a: 'a',
    A: 'A',
    M: 'M',
    MM: 'MM',
    MMM: 'MMM',
    MMMM: 'MMMM',
    h: 'h',
    hh: 'hh',
    H: 'H',
    HH: 'HH',
    m: 'm',
    mm: 'mm',
    s: 's',
    ss: 'ss',
    S: 'SSS',
    SS: 'SSS',
    SSS: 'SSS',
    E: 'ddd',
    EE: 'ddd',
    EEE: 'ddd',
    EEEE: 'dddd',
    EEEEE: 'dddd',
    EEEEEE: 'dddd',
    D: 'DDD',
    w: 'W',
    ww: 'WW',
    z: 'ZZ',
    zzzz: 'Z',
    Z: 'ZZ',
    X: 'ZZ',
    XX: 'ZZ',
    XXX: 'Z',
    u: 'E'
  };



  var toMomentFormat = function (formatString) {
      return translateFormat(formatString,javaFormatMapping);
  }
  var translateFormat = function (formatString, mapping) {
    var len = formatString.length;
    var i = 0;
    var startIndex = -1;
    var lastChar = null;
    var currentChar = "";
    var resultString = "";

    for (; i < len; i++) {
      currentChar = formatString.charAt(i);

      if (lastChar === null || lastChar !== currentChar) {
        // change detected
        resultString = appendMappedString(formatString, mapping, startIndex, i, resultString);

        startIndex = i;
      }

      lastChar = currentChar;
    }

    return appendMappedString(formatString, mapping, startIndex, i, resultString);
  };

  var appendMappedString = function (formatString, mapping, startIndex, currentIndex, resultString) {
    if (startIndex !== -1) {
      var tempString = formatString.substring(startIndex, currentIndex);

      if (mapping[tempString]) {
        tempString = mapping[tempString];
      }

      resultString += tempString;
    }

    return resultString;
  };

// set the date formatter to the datetimepicker so we can share the format between java code and the jquery picker.
$.datetimepicker.setDateFormatter({
                parseDate: function (date, format) {
                        format=toMomentFormat(format);
                        var d = moment(date, format);
                        return d.isValid() ? d.toDate() : false;
                    },

                    formatDate: function (date, format) {
                        format=toMomentFormat(format);
                        return moment(date).format(format);
                    }
            });

// init the date picker for the element with id = input id.
function initJQDatepicker(inputId,dateTime,dtformat,dtSep) {
            var args = new Object();
            args.timepicker=dateTime;
            args.format=dtformat;
            // the datepicker is not happy with only the full format
            // it wants as well date and time formats provided separately
            if (dateTime){
               var dtArr=dtformat.split(dtSep);
               args.formatTime=dtArr[1];
               args.formatDate=dtArr[0];
            } else {
               args.formatDate=dtformat;
            }
            $("#" + inputId).datetimepicker(args);
};