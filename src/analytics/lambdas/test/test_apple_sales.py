from importers import apple_sales
from . import check_code


def test_code_is_valid():
    assert check_code('importers/apple_sales.py')


