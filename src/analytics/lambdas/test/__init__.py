import ast


def check_code(file_name: str) -> bool:
    """ A Method which ensures that the code loaded is valid python using the abstract syntax tree (ast) parsing"""
    with open(file_name) as f:
        source = f.read()
    try:
        ast.parse(source)
        return True
    except SyntaxError:
        return False
