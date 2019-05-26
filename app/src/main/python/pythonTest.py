def test(location):
    print(location)
    file = open(location + "/test.txt","w+")

    file.write("Dit is een test")
    file.write(":)")

    file.close()