import cv2
import numpy as np
import struct

def main(save_location, picture_location):
    #make image grayscale
    img = cv2.imread(picture_location+'/Plattegrond_v4.jpg')
    im_gray = cv2.imread(picture_location+'/Plattegrond_v4.jpg', cv2.IMREAD_GRAYSCALE)

    # make image binary + define threshold when pixel is turned white or black
    (thresh, im_bw) = cv2.threshold(im_gray, 128, 255, cv2.THRESH_BINARY | cv2.THRESH_OTSU)
    thresh = 127
    im_bw = cv2.threshold(im_gray, thresh, 255, cv2.THRESH_BINARY)[1]

    #find all connected parts
    nb_components, output, stats, centroids = cv2.connectedComponentsWithStats(im_bw, connectivity=8)
    #connectedComponentswithStats yields every seperated component with information on each of them, such as size

    #remove background
    sizes = stats[1:, -1]; nb_components = nb_components - 1
    # minimum size of pixels we want to keep
    min_size = 1
    #your answer image
    img2 = np.zeros((output.shape))

    #for every component, keep only those above min_size
    for i in range(0, nb_components):
        if sizes[i] >= min_size:
            img2[output == i + 1] = 255

    #cv2.imshow('room detector', img2)

    #Morphological Transform
    kernel = np.ones((1, 1), np.uint8)
    dilation = cv2.dilate(img2, kernel)
    #erosion = cv2.erode(img2, kernel, iterations=6)

    #detect arrows
    # ret,thresh = cv2.threshold(im_gray,127,255,1)
    # contours,h = cv2.findContours(thresh,1,2)
    # for cnt in contours:
    #     approx = cv2.approxPolyDP(cnt,0.01*cv2.arcLength(cnt,True),True)
    #     if len(approx)>=8 and len(approx)<=12:
    #         cv2.drawContours(img,[cnt],0,255,-1)

    #create array and replace values
    img_array = np.array(dilation)
    img_array[img_array < 1] = 1
    img_array[img_array > 254] = 0

    np.savetxt(save_location+'/plattegrond.csv',img_array,fmt='%i', delimiter=',')

    #cv2.imwrite("Dilation.jpg", dilation)

def binary_save_test(save_location, picture_location):
    temp_value_array = [1,1,1,1,1,1,1,1,5,
                        1,1,1,1,1,1,1,1,5,
                        1,1,1,1,4,4,3,1,5,
						1,1,1,1,4,1,1,1,5,
						1,1,1,4,1,1,1,1,5,
						1,1,4,1,1,1,1,1,5,
						1,1,4,1,1,1,1,1,5,
						1,1,4,1,1,1,1,1,5,
						1,1,4,1,1,1,1,1,5,
                        1,1,1,2,1,1,1,1,5]

    with open(save_location+'/output.bin', 'wb') as f:
        for b in temp_value_array:
            f.write(struct.pack('b', b))