import cv2
import numpy as np
import struct
from pathfinding.core.diagonal_movement import DiagonalMovement
from pathfinding.core.grid import Grid
from pathfinding.finder.a_star import AStarFinder

def main(save_location, picture_location):
    def read_and_convert_image(path):
        img = cv2.imread(path, 0)
        cimg = cv2.cvtColor(img, cv2.COLOR_GRAY2BGR)
        return img, cimg

    def detect_start_and_end(img, cimg):
        startpoint = []
        endpoint = []
        circles = cv2.HoughCircles(img, cv2.HOUGH_GRADIENT, 1, 20,
                                   param1=50, param2=30, minRadius=0, maxRadius=0)
        circles2 = sorted(circles[0], key=lambda x: x[2], reverse=True)
        print(circles2)
        circles = np.uint16(np.around(circles))
        cv2.circle(cimg, (circles2[0][0], circles2[0][1]), circles2[0][2], (255, 255, 255), -1)
        cv2.circle(cimg, (circles2[1][0], circles2[1][1]), circles2[1][2], (255, 255, 255), -1)
        cv2.circle(cimg, (circles2[0][0], circles2[0][1]), 2, (255, 255, 255), 1)
        cv2.circle(cimg, (circles2[1][0], circles2[1][1]), 2, (255, 255, 255), 1)
        startpoint.append(circles2[0][0].astype(int))
        startpoint.append(circles2[0][1].astype(int))
        endpoint.append(circles2[1][0].astype(int))
        endpoint.append(circles2[1][1].astype(int))
        print(str("Found startpoint: x" + str(startpoint[0])) + " y" + str(startpoint[1]))
        print(str("Found endpoint: x" + str(endpoint[0])) + " y" + str(endpoint[1]))
        return startpoint, endpoint

    def filter_image(cimg):
        img_gray = cv2.cvtColor(cimg, cv2.COLOR_BGR2GRAY)
        (thresh, im_bw) = cv2.threshold(img_gray, 128, 255, cv2.THRESH_BINARY | cv2.THRESH_OTSU)
        thresh = 127
        im_bw = cv2.threshold(img_gray, thresh, 255, cv2.THRESH_BINARY)[1]

        nb_components, output, stats, centroids = cv2.connectedComponentsWithStats(im_bw, connectivity=8)
        sizes = stats[1:, -1]
        nb_components = nb_components - 1
        min_pixel_size = 1
        img2 = np.zeros((output.shape))

        for i in range(0, nb_components):
            if sizes[i] >= min_pixel_size:
                img2[output == i + 1] = 255

        return img2

    def morphological_transform(img2):
        kernel = np.ones((2, 2), np.uint8)
        dilation = cv2.dilate(img2, kernel)
        # erosion = cv2.erode(img2, kernel, iterations=6)
        return dilation

    def image_to_array(dilation):
        img_array = np.array(dilation)
        img_array[img_array <= 1] = 0
        img_array[img_array > 1] = 1
        img_array[img_array > 254] = 1
        print(img_array)
        return img_array

    def pathfinder(img_array, startpoint, endpoint):
        matrix = img_array
        grid = Grid(matrix=matrix)
        start = grid.node(startpoint[0], startpoint[1])
        end = grid.node(endpoint[0], endpoint[1])
        finder = AStarFinder(diagonal_movement=DiagonalMovement.always)
        path, runs = finder.find_path(start, end, grid)
        grid_str = grid.grid_str(path=path, start=start, end=end)
        print('operations:', runs, 'path length:', len(path))
        print(grid_str)
        img_array2 = img_array
        for node in path:
            col = node[0]
            row = node[1]
            img_array2[row][col]= 4
        img_array2[startpoint[1]][startpoint[0]] = 2
        img_array2[endpoint[1]][endpoint[0]] = 3
        return img_array2

    img, cimg = read_and_convert_image(picture_location)
    startpoint, endpoint = detect_start_and_end(img, cimg)
    img2 = filter_image(cimg)
    dilation = morphological_transform(img2)
    img_array = image_to_array(dilation)
    array_w_path = pathfinder(img_array, startpoint, endpoint)

    arr = []

    for row in array_w_path:
        for val in row:
            arr.append(int(val))
        arr.append(int(5))
        print(len(row))
        print(row[-1])

    #np.savetxt('plattegrond_pf.csv', array_w_path, fmt='%i', delimiter=',')

    with open(save_location+'/output.bin', 'wb') as f:
        for b in arr:
            f.write(struct.pack('b', b))
