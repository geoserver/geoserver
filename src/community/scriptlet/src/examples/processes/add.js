metadata = {
    title: "Addition Example",
    description: "Example script that adds two numbers together",
    inputs: {
        lhs: java.lang.Integer,
        rhs: java.lang.Integer
    },
    outputs: {
        sum: java.lang.Integer
    }
}

function process(input) {
    return {
        sum: (input.lhs + input.rhs)
    };
}
