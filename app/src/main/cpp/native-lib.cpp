#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>
#include <vector>
#include <string>
#include <sstream>

using namespace cv;
using namespace std;

////
//Default Functions for Detecting Human
////

Mat preprocessing(Mat image) {
    Mat gray;
    cvtColor(image, gray, CV_BGR2GRAY);
    equalizeHist(gray, gray);

    return gray;
}

void setHumanRect(Mat &img, HOGDescriptor hog){
    vector<Rect> found, found_filtered;
    double t = (double)getTickCount();
    // run the detector with default parameters. to get a higher hit-rate
    // (and more false alarms, respectively), decrease the hitThreshold and
    // groupThreshold (set groupThreshold to 0 to turn off the grouping completely).
    int can = img.channels();
    hog.detectMultiScale(img, found, 0, Size(8, 8), Size(32, 32), 1.05, 2);
    size_t i, j;
    for (i = 0; i < found.size(); i++) {
        Rect r = found[i];
        for (j = 0; j < found.size(); j++)
            if (j != i && (r & found[j]) == r)
                break;
        if (j == found.size())
            found_filtered.push_back(r);
    }
    for (i = 0; i < found_filtered.size(); i++) {
        Rect r = found_filtered[i];
        // the HOG detector returns slightly larger rectangles than the real objects.
        // so we slightly shrink the rectangles to get a nicer output.
        r.x += cvRound(r.width*0.1);
        r.width = cvRound(r.width*0.8);
        r.y += cvRound(r.height*0.07);
        r.height = cvRound(r.height*0.8);
        rectangle(img, r.tl(), r.br(), cv::Scalar(0, 255, 0), 3);
    }
}

Rect getHumanUpperBodyRect(Rect face)
{
    Rect UpperBody;

    UpperBody.width = face.width * 3;
    UpperBody.height = face.height * 2.5;

    UpperBody.x = face.x - face.width;
    UpperBody.y = face.y + face.height * 1.1 + face.height*0.25;

    return UpperBody;
}

Rect getHumanLowerBodyRect(Rect face, Rect UpperBody)
{
    Rect LowerBody;
    LowerBody.width = face.width * 3;
    LowerBody.height = face.height * 4;

    LowerBody.x = face.x - face.width*0.8;
    LowerBody.y = UpperBody.br().y + face.height*0.25;

    return LowerBody;
}

void setBackground(Mat &image){
    for (int j = 0; j < image.rows; j++) {
        for (int i = 0; i < image.cols; i++) {
            if (image.at<Vec3b>(j, i)[0] == 255 && image.at<Vec3b>(j, i)[1] == 255 && image.at<Vec3b>(j, i)[2] == 255)
                image.at<Vec3b>(j, i)[0] = 254;
        }
    }

    for (int j = 0; j < image.rows; j++) {
        for (int i = 0; i < image.cols; i++) {
            if (image.at<Vec3b>(j, i)[0] == 0 && image.at<Vec3b>(j, i)[1] == 0 && image.at<Vec3b>(j, i)[2] == 0)
                image.at<Vec3b>(j, i)[0] = 1;
        }
    }
}

////
//END of Default fashionProcess
////

////
//Convert / Detect Functions
////

typedef struct values {
    Vec3b rgb;
    int cnt;
} value;

template <typename T2>
T2 Convert3(T2 x) {
    if (0 <= x && x < 44) {
        x = 10;
        return x;
    }
    else if (44 <= x && x < 64) {
        x = 53;
        return x;
    }
    else if (64 <= x && x < 84) {
        x = 73;
        return x;
    }
    else if (84 <= x && x < 104) {
        x = 93;
        return x;
    }
    else if (104 <= x && x < 124) {
        x = 113;
        return x;
    }
    else if (124 <= x && x < 144) {
        x = 133;
        return x;
    }
    else if (144 <= x && x < 164) {
        x = 153;
        return x;
    }
    else if (164 <= x && x < 184) {
        x = 173;
        return x;
    }
    else if (184 <= x && x < 204) {
        x = 193;
        return x;
    }
    else if (204 <= x && x < 224) {
        x = 213;
        return x;
    }
    else {
        x = 245;
        return x;
    }
}


Vec3b detectcolor(Mat &image, int row, int col) {
    int blue, green, red, t_blue = 0, t_green = 0, t_red = 0;
    blue = 0; green = 0; red = 0;
    int cnt = 0;

    for (int j = row; j < row + 5; j++) {
        for (int i = col; i < col + 5; i++) {
            if (image.at<Vec3b>(j, i)[0] == 255 && image.at<Vec3b>(j, i)[1] == 255 && image.at<Vec3b>(j, i)[2] == 255)
                continue;

            blue = image.at<Vec3b>(row, col)[0]; // blue
            green = image.at<Vec3b>(row, col)[1]; // green
            red = image.at<Vec3b>(row, col)[2]; // red

            t_blue += blue;
            t_green += green;
            t_red += red;
            cnt++;
        }
    }

    if (cnt > 0) {
        t_blue /= cnt; t_green /= cnt; t_red /= cnt;

        return Vec3b(Convert3(t_blue), Convert3(t_green), Convert3(t_red));
    }

    else {
        return Vec3b(0, 0, 0);
    }

}


void putValue(vector<value> &arr, Vec3b color) {
    int count = 0;
    for (size_t i = 0; i < arr.size(); ++i) {
        count = 0;
        int count2 = 0;
        for (int j = 0; j < 3; j++) {
            if (arr[i].rgb[j] == color[j]) {
                count2++;
            }
        }
        if (count2 == 3) {
            count++;
            arr[i].cnt++;
            break;
        }
    }
    value v1;
    if (count == 0) {
        v1.rgb = color;
        v1.cnt = 0;
        arr.push_back(v1);
    }
}


double modd(double a, int b) {
    while (a >= b) {
        a -= b;
    }
    return a;
}


void FindMaxCnt(vector<value> &arr, vector<value> &M_arr) {

    int max = 0, index = 0;

    for (int i = 0; i < arr.size(); i++) {
        if (max < arr[i].cnt) {
            max = arr[i].cnt;
            index = i;
        }
    }

    value v1;
    v1.rgb = arr[index].rgb;
    v1.cnt = arr[index].cnt;
    M_arr.push_back(v1);

}


void DeleteSkinColor(vector<value> &arr, vector<value> &S_arr) {

    int blue, green, red;

    for (int i = 0; i < arr.size(); i++) {
        blue = arr[i].rgb[0]; // blue
        green = arr[i].rgb[1]; // green
        red = arr[i].rgb[2]; // red

        if (red > 95 && green > 40 && blue > 20 && max(red, max(green, blue)) - min(red, min(green, blue)) > 15 && red > green && red > blue) {
            continue;
        }

        value v1;
        v1.rgb = arr[i].rgb;
        v1.cnt = arr[i].cnt;
        S_arr.push_back(v1);
    }
}

//hsv->rgb
void HSVtoRGB(double h, double s, double v, int &red, int &green, int &blue){

    double C, X, M;

    C = s * v;
    X = C * (1 - abs(modd((h / 60), 2) - 1));
    M = v - C;		// hsv -> rgb

    if (0 <= h && h < 60) {
        red = int((C + M) * 255);
        green = int((X + M) * 255);
        blue = int((M + 0) * 255);
    }
    else if (60 <= h && h < 120) {
        red = int((X + M) * 255);
        green = int((C + M) * 255);
        blue = int((0 + M) * 255);
    }
    else if (120 <= h && h < 180) {
        red = int((0 + M) * 255);
        green = int((C + M) * 255);
        blue = int((X + M) * 255);
    }
    else if (180 <= h && h < 240) {
        red = int((0 + M) * 255);
        green = int((X + M) * 255);
        blue = int((C + M) * 255);
    }
    else if (240 <= h && h < 300) {
        red = int((X + M) * 255);
        green = int((0 + M) * 255);
        blue = int((C + M) * 255);
    }
    else {
        red = int((C + M) * 255);
        green = int((0 + M) * 255);
        blue = int((X + M) * 255);
    }

}


Mat ToneOnTone(vector<value> &arr) {

    Mat Up(300, 300, CV_8UC3, Scalar(245, 245, 245));

    double b, g, r, h, s, v;
    b = 0; g = 0; r = 0, h = 0, s = 0, v = 0;
    double C, X, M;
    int red, green, blue, l = 0, k = 0;
    double b_, g_, r_, Cmax, Cmin, delta;

    for (int i = 0; i < arr.size(); i++) {

        b = arr[i].rgb[0];
        g = arr[i].rgb[1];
        r = arr[i].rgb[2];

        b_ = b / 255;
        g_ = g / 255;
        r_ = r / 255;
        Cmax = max(b_, max(g_, r_));
        Cmin = min(b_, min(g_, r_));

        delta = Cmax - Cmin;

        if (delta == 0) {
            h = 0;
        }
        else if (Cmax == r_) {
            h = 60 * (modd((g_ - b_) / delta, 6));
        }
        else if (Cmax == g_) {
            h = 60 * ((b_ - r_) / delta + 2);
        }
        else {
            h = 60 * ((r_ - g_) / delta + 4);
        }


        if (Cmax == 0) {
            s = 0;
        }
        else {
            s = delta / Cmax;
        }

        v = Cmax;


        if (v > 0.7) {
            double s1 = s;

            for (float v1 = v; v1 < 1; v1 += 0.08) {

                HSVtoRGB(h, s1, v1, red, green, blue);

                circle(Up, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }

            for (float v1 = v; v1 > 0.08; v1 -= 0.08) {

                HSVtoRGB(h, s1, v1, red, green, blue);

                circle(Up, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }
        }

        else if (v < 0.3) {
            double s1 = s;

            for (float v1 = v; v1 < 1; v1 += 0.08) {

                HSVtoRGB(h, s1, v1, red, green, blue);

                circle(Up, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }
        }

        else {
            double s1 = s;
            for (float v1 = v; v1 < 1; v1 += 0.07) {

                HSVtoRGB(h, s1, v1, red, green, blue);

                circle(Up, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }


            for (float v1 = v; v1 > 0.3; v1 -= 0.08) {

                HSVtoRGB(h, s1, v1, red, green, blue);

                circle(Up, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }
        }

        k += 200;
        l = 0;
    }

    return Up;
}


Mat ToneInTone(vector<value> &arr) {

    Mat Up2(300, 300, CV_8UC3, Scalar(255, 245, 245));

    double b, g, r, h, s, v;
    b = 0; g = 0; r = 0, h = 0, s = 0, v = 0;
    double C, X, M;
    int red, green, blue, l = 0, k = 0;
    double b_, g_, r_, Cmax, Cmin, delta;

    for (int i = 0; i < arr.size(); i++) {

        b = arr[i].rgb[0];
        g = arr[i].rgb[1];
        r = arr[i].rgb[2];

        b_ = b / 255;
        g_ = g / 255;
        r_ = r / 255;

        Cmax = max(b_, max(g_, r_));
        Cmin = min(b_, min(g_, r_));

        delta = Cmax - Cmin;

        if (delta == 0) {
            h = 0;
        }
        else if (Cmax == r_) {
            h = 60 * (modd((g_ - b_) / delta, 6));
        }
        else if (Cmax == g_) {
            h = 60 * ((b_ - r_) / delta + 2);
        }
        else {
            h = 60 * ((r_ - g_) / delta + 4);
        }


        if (Cmax == 0) {
            s = 0;
        }
        else {
            s = delta / Cmax;
        }

        v = Cmax;

        double s1 = s;
        double v1 = v;


        if (s < 0.6 && v < 0.35) {
            s1 = 0.6;
            v1 = 0.35;
            for (double h1 = 0; h1 < 360; h1 += 20) {

                HSVtoRGB(h1, s1, v1, red, green, blue);

                circle(Up2, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }
        }

        else if (s >= 60 && v < 35) {
            v1 = 0.35;
            for (double h1 = 0; h1 < 360; h1 += 20) {

                HSVtoRGB(h1, s1, v1, red, green, blue);

                circle(Up2, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }
        }

        else if (0.35 <= v && v < 0.42 && s < 0.6) {
            s1 = 0.6;

            for (double h1 = 0; h1 < 360; h1 += 20) {

                HSVtoRGB(h1, s1, v1, red, green, blue);

                circle(Up2, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }
        }

        else if (0.42 <= v && v < 0.49 && s < 0.55) {
            s1 = 0.55;

            for (double h1 = 0; h1 < 360; h1 += 20) {

                HSVtoRGB(h1, s1, v1, red, green, blue);

                circle(Up2, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }
        }

        else if (0.49 <= v && v < 0.56 && s < 0.5) {
            s1 = 0.5;

            for (double h1 = 0; h1 < 360; h1 += 20) {

                HSVtoRGB(h1, s1, v1, red, green, blue);

                circle(Up2, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }
        }

        else if (0.56 <= v && v < 0.63 && s < 0.45) {
            s1 = 0.45;

            for (double h1 = 0; h1 < 360; h1 += 20) {

                HSVtoRGB(h1, s1, v1, red, green, blue);

                circle(Up2, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }
        }

        else if (0.63 <= v && v < 0.7 && s < 0.4) {
            s1 = 0.4;

            for (double h1 = 0; h1 < 360; h1 += 20) {

                HSVtoRGB(h1, s1, v1, red, green, blue);

                circle(Up2, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }
        }

        else if (0.7 <= v && v < 0.77 && s < 0.35) {
            s1 = 0.35;

            for (double h1 = 0; h1 < 360; h1 += 20) {

                HSVtoRGB(h1, s1, v1, red, green, blue);

                circle(Up2, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }
        }

        else if (0.77 <= v && v < 0.84 && s < 0.3) {
            s1 = 0.3;

            for (double h1 = 0; h1 < 360; h1 += 20) {

                HSVtoRGB(h1, s1, v1, red, green, blue);

                circle(Up2, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }
        }

        else if (0.84 <= v && v < 0.91 && s < 0.25) {
            s1 = 0.25;

            for (double h1 = 0; h1 < 360; h1 += 20) {

                HSVtoRGB(h1, s1, v1, red, green, blue);

                circle(Up2, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }
        }

        else if (0.91 <= v && s < 0.2) {
            s1 = 0.2;

            for (double h1 = 0; h1 < 360; h1 += 20) {

                HSVtoRGB(h1, s1, v1, red, green, blue);

                circle(Up2, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }
        }

        else {
            for (double h1 = 0; h1 < 360; h1 += 20) {

                HSVtoRGB(h1, s1, v1, red, green, blue);

                circle(Up2, Point(10 + l, 10 + k), 20, Scalar(Convert3(blue), Convert3(green), Convert3(red)), -1);
                l += 20;
            }
        }

        k += 200;
        l = 0;
    }

    return Up2;
}

////
//END of detect skin, cloth, etc...
////

bool compareVec3b(Vec3b obj1, Vec3b obj2){

    for(int i=0; i<3; i++){
        if(obj1[i] != obj2[i]){
            return false;
        }
    }

    return true;
}


extern "C"{
JNIEXPORT void JNICALL
Java_qol_fashionchecker_ResultActivity_loadImage(JNIEnv *env, jobject instance,
                                               jstring imageFileName_, jlong addrImage) {
    // TODO
    Mat &img_input = *(Mat *) addrImage;

    const char *nativeFileNameString = env->GetStringUTFChars(imageFileName_, JNI_FALSE);

    String baseDir = nativeFileNameString;
    const char *pathDir = baseDir.c_str();

    img_input = imread(pathDir, IMREAD_COLOR);
}


//Cascade Load
JNIEXPORT jlong JNICALL
Java_qol_fashionchecker_ResultActivity_loadCascade(JNIEnv *env, jobject instance,
                                                 jstring cascadeFileName_) {

    // TODO
    const char *nativeFileNameString = env->GetStringUTFChars(cascadeFileName_, 0);
    string baseDir("/storage/emulated/0/");
    baseDir.append(nativeFileNameString);
    const char *pathDir = baseDir.c_str();
    jlong ret = 0;
    ret = (jlong) new CascadeClassifier(pathDir);
    if (((CascadeClassifier *) ret)->empty()) {
        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                            "CascadeClassifier로 로딩 실패  %s", nativeFileNameString);
    }
    else
        __android_log_print(ANDROID_LOG_DEBUG, "native-lib :: ",
                            "CascadeClassifier로 로딩 성공 %s", nativeFileNameString);

    env->ReleaseStringUTFChars(cascadeFileName_, nativeFileNameString);
    return ret;
}

//check Fashion
JNIEXPORT void JNICALL
Java_qol_fashionchecker_ResultActivity_checkFashion(JNIEnv *env, jobject type,
                                                    jlong cascadeClassifier_face,
                                                    jlong matAddrInput,
                                                    jlong matAddrResult1,
                                                    jlong matAddrResult2,
                                                    jlong matAddrResult3,
                                                    jlong matAddrResult4,
                                                    jlong matAddrResult5,
                                                    jlong matAddrResult6) {

    Mat &img_input = *(Mat *) matAddrInput;
    Mat &img_result1 = *(Mat *) matAddrResult1;
    Mat &img_result2 = *(Mat *) matAddrResult2;
    Mat &img_result3 = *(Mat *) matAddrResult3;
    Mat &img_result4 = *(Mat *) matAddrResult4;
    Mat &img_result5 = *(Mat *) matAddrResult5;
    Mat &img_result6 = *(Mat *) matAddrResult6;
    // TODO Something with img_result

    Mat image = img_input.clone();

    //Set Hog for Human Detection
    HOGDescriptor hog;
    hog.setSVMDetector(HOGDescriptor::getDefaultPeopleDetector());

    //Change White to non-white to distinguish background from cloth.
    setBackground(image);

    Mat gray = preprocessing(image);

    //-- Detect faces
    vector<Rect> face;
    ((CascadeClassifier *) cascadeClassifier_face)->detectMultiScale(gray, face, 1.1, 2, 0|CASCADE_SCALE_IMAGE, Size(30, 30) );
    Rect Face = face[0];
    rectangle(image, Face.tl(), Face.br(), Scalar(0, 0, 255), 2);

    Rect UpperBody, LowerBody;
    UpperBody = getHumanUpperBodyRect(Face);
    LowerBody = getHumanLowerBodyRect(Face, UpperBody);


    Rect temp1;
    temp1.x = UpperBody.x + 5;
    temp1.y = UpperBody.y;
    temp1.width = UpperBody.width - 15;
    temp1.height = UpperBody.height - 5;

    Mat B(image, Rect(0, 0, temp1.x, image.rows));
    Mat C(image, Rect(temp1.x + temp1.width, 0, image.cols - (temp1.x + temp1.width), image.rows));
    Mat D(image, Rect(0, 0, image.cols, temp1.y));

    Rect temp2, temp22;
    temp2.x = temp1.x + 20;
    temp2.y = temp1.y + 20;
    temp2.width = temp1.width - 40;
    temp2.height = temp1.height - 40;

    temp22.x = LowerBody.x + 20;
    temp22.y = LowerBody.y + 30;
    temp22.width = LowerBody.width - 40;
    temp22.height = LowerBody.height - 50;

    blur(B, B, Size(7, 7));
    blur(C, C, Size(7, 7));
    blur(D, D, Size(7, 7));

    Mat bgModel, fgModel;
    Mat result, result2;

    grabCut(image,
            result,
            UpperBody,
            bgModel, fgModel,
            2,
            GC_INIT_WITH_RECT);

    compare(result, GC_PR_FGD, result, CMP_EQ);

    Mat cloth_foreground(image.size(), CV_8UC3, Scalar(255, 255, 255));

    image.copyTo(cloth_foreground, result);

    grabCut(image,
            result2,
            LowerBody,
            bgModel, fgModel,
            5, GC_INIT_WITH_RECT);

    compare(result2, GC_PR_FGD, result2, CMP_EQ);

    Mat pants_foreground(image.size(), CV_8UC3, Scalar(255, 255, 255));

    image.copyTo(pants_foreground, result2);


    Mat M2(cloth_foreground, temp2);
    Mat M3(pants_foreground, temp22);


    vector<value> Up_colorArray;
    double totalcnt = 0;

    for (int j = 5; j < M2.rows - 5; j += 5) {
        for (int i = 5; i <M2.cols - 5; i += 5) {
            if (compareVec3b(detectcolor(M2, j, i), Vec3b(0,0,0))) {
                continue;
            }
            putValue(Up_colorArray, detectcolor(M2, j, i));
            totalcnt++;
        }
    }

    vector<value> Max_Up_colorArray, Skin_Up_colorArray;

    FindMaxCnt(Up_colorArray, Max_Up_colorArray);

    int k = 0; int l = 0;
    Mat Up_colorchip(300, 300, CV_8UC3, Scalar(255, 255, 255));
    Mat Up_colorchip2(300, 300, CV_8UC3, Scalar(255, 255, 255));
    Mat Up_colorchip3(300, 300, CV_8UC3, Scalar(255, 255, 255));


    for (size_t i = 0; i < Up_colorArray.size(); ) {
        int percent = (Up_colorArray[i].cnt / totalcnt) * 100;
        stringstream s;
        s << percent;
        string str = s.str();

        if (percent <= 5) {
            Up_colorArray.erase(Up_colorArray.begin() + i);
            continue;
        }

        circle(Up_colorchip, Point(10, 10 + l), 10, Up_colorArray[i].rgb, -1);

        putText(Up_colorchip, str, Point(27, 15 + l), 1, 1, Scalar(0, 0, 0));
        l += 30;
        i++;
    }

    circle(Up_colorchip2, Point(10, 10), 10, Max_Up_colorArray[0].rgb, -1);

    DeleteSkinColor(Up_colorArray, Skin_Up_colorArray);

    l = 0;
    for (size_t i = 0; i < Skin_Up_colorArray.size(); ) {

        circle(Up_colorchip3, Point(10, 10 + l), 10, Skin_Up_colorArray[i].rgb, -1);

        l += 30;
        i++;
    }

    Mat Up_TOT(300, 100, CV_8UC3, Scalar(255, 255, 255));
    Mat Up_TIT(300, 100, CV_8UC3, Scalar(255, 255, 255));

    Up_TOT = ToneOnTone(Max_Up_colorArray);
    Up_TIT = ToneInTone(Max_Up_colorArray);


    totalcnt = 0, l = 0;
    vector<value> Lo_colorArray;

    for (int j = 5; j < M3.rows - 10; j += 5) {
        for (int i = 5; i <M3.cols - 5; i += 5) {
            if (compareVec3b(detectcolor(M3, j, i), Vec3b(0, 0, 0))) {
                continue;
            }
            putValue(Lo_colorArray, detectcolor(M3, j, i));
            totalcnt++;
        }
    }

    vector<value> Max_Lo_colorArray, Skin_Lo_colorArray;

    FindMaxCnt(Lo_colorArray, Max_Lo_colorArray);

    Mat Lo_colorchip(300, 300, CV_8UC3, Scalar(255, 255, 255));
    Mat Lo_colorchip2(300, 300, CV_8UC3, Scalar(255, 255, 255));
    Mat Lo_colorchip3(300, 300, CV_8UC3, Scalar(255, 255, 255));

    for (size_t i = 0; i < Lo_colorArray.size(); ) {
        int percent = (Lo_colorArray[i].cnt / totalcnt) * 100;
        stringstream s;
        s << percent;
        string str = s.str();

        if (percent <= 5) {
            Lo_colorArray.erase(Lo_colorArray.begin() + i);
            continue;
        }

        circle(Lo_colorchip, Point(10, 10 + l), 10, Lo_colorArray[i].rgb, -1);

        putText(Lo_colorchip, str, Point(27, 15 + l), 1, 1, Scalar(0, 0, 0));
        l += 30;
        i++;
    }

    circle(Lo_colorchip2, Point(10, 10), 10, Max_Lo_colorArray[0].rgb, -1);

    DeleteSkinColor(Lo_colorArray, Skin_Lo_colorArray);

    l = 0;
    for (size_t i = 0; i < Skin_Lo_colorArray.size(); ) {
        circle(Lo_colorchip3, Point(10, 10 + l), 10, Skin_Lo_colorArray[i].rgb, -1);

        l += 30;
        i++;
    }

    Mat Lo_TOT(300, 100, CV_8UC3, Scalar(255, 255, 255));
    Mat Lo_TIT(300, 100, CV_8UC3, Scalar(255, 255, 255));

    Lo_TOT = ToneOnTone(Max_Lo_colorArray);
    Lo_TIT = ToneInTone(Max_Lo_colorArray);

    /*
     * img_result1~6 Setting -> becomes the bitmap Value.
     */
    img_result1 = Up_TOT;
    img_result2 = Up_TIT;
    img_result3 = Lo_TOT;
    img_result4 = Lo_TIT;
    img_result5 = Up_TOT;
    img_result6 = Lo_TIT;
}

}