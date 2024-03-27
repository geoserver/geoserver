# Directives

A directive is a CSS top level declaration that allows control of some aspects of the stylesheet application or translation to SLD. All directives are declared at the beginning of the CSS sheet and follow the same syntax:

``` css
@name value;
```

For example:

``` scss
@mode 'Flat';
@styleName 'The name';
@styleTitle 'The title;
@styleAbstract 'This is a longer description'

* { 
  stroke: black 
}

[cat = 10] { 
  stroke: yellow; stroke-width: 10 
}
```

## Supported directives

| Directive       | Type                                          | Meaning                                                                                                                                                                                                                                                                                    | Accepts Expression? |
|-----------------|-----------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------|
| `mode`          | String, `Exclusive`, `Simple`, `Auto`, `Flat` | Controls how the CSS is translated to SLD. `Exclusive`, `Simple` and `Auto` are cascaded modes, `Flat` turns off cascading and has the CSS behave like a simplified syntax SLD sheet. See [Understanding Cascading in CSS](cascading.md) for an explanation of how the various modes work | false               |
| `styleName`     | String                                        | The generated SLD style name                                                                                                                                                                                                                                                               | No                  |
| `styleTitle`    | String                                        | The generated SLD style title                                                                                                                                                                                                                                                              | No                  |
| `styleAbstract` | String                                        | The generated SLD style abstract/description                                                                                                                                                                                                                                               | No                  |
