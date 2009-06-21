public class Test {
        public static void main(String[] args) {
                try {
                        HighguiLibrary.IplImage img = HighguiLibrary.INSTANCE.cvLoadImage(args[0], CV_LOAD_IMAGE_UNCHANGED);
                        HighguiLibrary.INSTANCE.cvNamedWindow("Example1", CV_WINDOW_AUTOSIZE);
                        HighguiLibrary.INSTANCE.cvShowImage("Example1", new HighguiLibrary.CvArr(img));
                        System.in.read();
                        //HighguiLibrary.INSTANCE.cvWaitKey(0);
                        CxcoreLibrary.INSTANCE.cvReleaseImage(new PointerByReference(img.getPointer(0)));
                        HighguiLibrary.INSTANCE.cvDestroyWindow("Example1");
                } catch (Throwable th) {
                        th.printStackTrace();
                }
        }

}
