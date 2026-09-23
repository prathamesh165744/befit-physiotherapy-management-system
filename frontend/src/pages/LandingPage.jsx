import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

// Import your local slider images
import slide1 from '../assets/slider_home1.jpg';
import slide2 from '../assets/slider_home2.jpg';
import slide3 from '../assets/slider_home3.jpg';
import slide4 from '../assets/slider_home4.jpg';

export default function LandingPage() {
  const [showBranches, setShowBranches] = useState(false);
  const [currentSlide, setCurrentSlide] = useState(0);
  
  const slides = [slide1, slide2, slide3, slide4];

  // Auto-advance the slider every 5 seconds
  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentSlide((prevSlide) => (prevSlide + 1) % slides.length);
    }, 5000);
    return () => clearInterval(timer);
  }, [slides.length]);

  return (
    <div className="min-h-screen bg-slate-50 font-sans text-slate-800">
      
      {/* Sticky Emergency Button */}
      <div className="fixed right-0 top-1/2 -translate-y-1/2 z-50">
        <div className="bg-red-600 text-white transform rotate-180 px-2 py-8 text-sm font-bold tracking-widest cursor-pointer hover:bg-red-700 shadow-xl rounded-l-md" style={{ writingMode: 'vertical-rl' }}>
          EMERGENCY 24/7
        </div>
      </div>

      {/* Topmost Info Bar */}
      <div className="bg-blue-900 text-white text-xs py-2 px-4 flex justify-between items-center max-w-full">
        <div className="hidden md:block ml-4">
          <span className="font-semibold">📞 +91 20 6645 5100</span>
        </div>
        <div className="flex space-x-6 mr-4">
          <span className="hover:text-blue-200 cursor-pointer">care@befitphysiotherapy.com</span>
          <span className="hover:text-blue-200 cursor-pointer">Working Hours: 8:00 AM - 9:00 PM</span>
        </div>
      </div>

      {/* Main Navbar */}
      <header className="bg-white shadow-sm sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 md:px-8 py-4 flex justify-between items-center">
          
          <div className="flex items-center space-x-3">
            <div className="w-10 h-12 flex items-center justify-center">
               <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-12 h-12 text-blue-600">
                  <path d="M12 2C10.8954 2 10 2.89543 10 4C10 5.10457 10.8954 6 12 6C13.1046 6 14 5.10457 14 4C14 2.89543 13.1046 2 12 2Z" fill="currentColor"/>
                  <path d="M14.8284 10.1716C13.7916 9.13474 12.5 8 10 8C7.79086 8 6 9.79086 6 12V14H8V12C8 10.8954 8.89543 10 10 10C11.5 10 12.3787 10.8787 13.4142 11.9142L14.8284 13.3284C15.8653 14.3653 17.1569 15.5 19.6569 15.5V13.5C17.866 13.5 16.9873 12.6213 15.9518 11.5858L14.8284 10.1716Z" fill="currentColor"/>
                  <path d="M8 22V16H10V22H8Z" fill="currentColor"/>
                  <path d="M14 22V16H16V22H14Z" fill="currentColor"/>
               </svg>
            </div>
            <div>
              <div className="text-blue-900 text-2xl font-bold leading-none">BeFit</div>
              <div className="text-blue-600 text-xs font-medium tracking-wide">Physiotherapy Clinic</div>
            </div>
          </div>
          
          <div className="hidden md:flex items-center space-x-8 text-sm font-medium text-slate-700">
            <div className="relative">
              <button 
                onClick={() => setShowBranches(!showBranches)}
                className="flex items-center space-x-1 text-slate-600 hover:text-blue-600 transition"
              >
                <span>📍 Select Branch</span>
                <span className="text-[10px]">▼</span>
              </button>
              
              {showBranches && (
                <div className="absolute top-full right-0 mt-1 w-64 bg-white border border-gray-200 rounded-md shadow-xl z-50 overflow-hidden">
                  <div className="p-3 border-b border-gray-100 hover:bg-slate-50 cursor-pointer">
                    <div className="font-bold text-blue-900">Karve Road Branch</div>
                    <div className="text-gray-500 text-xs">+91 20 1234 5678</div>
                  </div>
                  <div className="p-3 border-b border-gray-100 hover:bg-slate-50 cursor-pointer">
                    <div className="font-bold text-blue-900">Paud Road Branch</div>
                    <div className="text-gray-500 text-xs">+91 20 8765 4321</div>
                  </div>
                  <div className="p-3 hover:bg-slate-50 cursor-pointer">
                    <div className="font-bold text-blue-900">Baner Road Branch</div>
                    <div className="text-gray-500 text-xs">+91 20 1122 3344</div>
                  </div>
                </div>
              )}
            </div>
            
            <a href="#services" className="hover:text-blue-600 transition">Services</a>
            <a href="#about" className="hover:text-blue-600 transition">About Us</a>
            <Link to="/staff-dashboard" className="text-slate-500 hover:text-blue-600 transition">Staff Portal</Link>
            
            <Link to="/login" className="bg-blue-600 text-white px-6 py-2.5 rounded-lg hover:bg-blue-700 shadow-md shadow-blue-200 transition font-semibold">
              Book Appointment
            </Link>
          </div>
        </div>
      </header>

      {/* Full-Width Hero Slider Section */}
      <section className="relative w-full h-[600px] md:h-[750px] flex items-center overflow-hidden">
        
        {/* Background Images */}
        {slides.map((slide, index) => (
          <img
            key={index}
            src={slide}
            alt={`Clinic facility ${index + 1}`}
            className={`absolute inset-0 w-full h-full object-cover transition-opacity duration-1000 ease-in-out ${
              index === currentSlide ? 'opacity-100 z-0' : 'opacity-0 -z-10'
            }`}
          />
        ))}

        {/* Thin Mask / Gradient Overlay */}
        <div className="absolute inset-0 bg-gradient-to-r from-slate-900/80 via-slate-900/50 to-transparent z-10"></div>
        <div className="absolute inset-0 bg-blue-900/20 mix-blend-multiply z-10"></div>

        {/* Floating Text Content */}
        <div className="relative z-20 max-w-7xl mx-auto px-4 md:px-8 w-full">
          <div className="max-w-2xl">
            <div className="inline-block px-4 py-1.5 bg-blue-500/20 text-blue-100 font-semibold text-xs rounded-full mb-6 tracking-widest uppercase border border-blue-300/30 backdrop-blur-md">
              Move Better | Live Healthier
            </div>
            <h1 className="text-4xl md:text-5xl lg:text-7xl font-extrabold text-white leading-tight mb-6 drop-shadow-lg">
              Your Journey <br/>
              <span className="text-blue-400">to Better Health</span> Starts Here
            </h1>
            <p className="text-lg md:text-xl text-blue-50 mb-10 leading-relaxed max-w-lg drop-shadow-md">
              Advanced physiotherapy care, personalized treatment plans and expert guidance — for a healthier, stronger you.
            </p>
            
            {/* Feature Badges adapted for dark background */}
            <div className="flex flex-wrap gap-4 mb-10">
              <div className="flex items-center space-x-2 bg-white/10 backdrop-blur-md border border-white/20 pr-4 py-1 pl-1 rounded-full shadow-lg">
                <div className="w-8 h-8 bg-white text-blue-600 rounded-full flex items-center justify-center text-sm font-bold">👨‍⚕️</div>
                <span className="text-xs font-semibold text-white">Expert Therapists</span>
              </div>
              <div className="flex items-center space-x-2 bg-white/10 backdrop-blur-md border border-white/20 pr-4 py-1 pl-1 rounded-full shadow-lg">
                <div className="w-8 h-8 bg-white text-blue-600 rounded-full flex items-center justify-center text-sm font-bold">🏥</div>
                <span className="text-xs font-semibold text-white">Multiple Branches</span>
              </div>
              <div className="flex items-center space-x-2 bg-white/10 backdrop-blur-md border border-white/20 pr-4 py-1 pl-1 rounded-full shadow-lg">
                <div className="w-8 h-8 bg-white text-blue-600 rounded-full flex items-center justify-center text-sm font-bold">💙</div>
                <span className="text-xs font-semibold text-white">Personalized Care</span>
              </div>
            </div>

            <div className="flex space-x-4">
              <Link to="/login" className="bg-blue-600 hover:bg-blue-500 text-white font-semibold py-4 px-10 rounded-lg shadow-lg shadow-blue-900/50 transition transform hover:-translate-y-1">
                Start Recovery Today
              </Link>
            </div>
          </div>
        </div>

        {/* Slider Indicators (Dots) */}
        <div className="absolute bottom-8 left-0 right-0 z-20 flex justify-center space-x-3">
          {slides.map((_, index) => (
            <button
              key={index}
              onClick={() => setCurrentSlide(index)}
              className={`w-3 h-3 rounded-full transition-all duration-300 ${
                index === currentSlide ? 'bg-blue-500 w-8' : 'bg-white/50 hover:bg-white/80'
              }`}
              aria-label={`Go to slide ${index + 1}`}
            />
          ))}
        </div>
      </section>

      {/* About / Banner Section */}
      <section className="bg-blue-900 py-16 text-white" id="about">
        <div className="max-w-7xl mx-auto px-4 md:px-8 grid grid-cols-1 md:grid-cols-4 gap-8 text-center divide-y md:divide-y-0 md:divide-x divide-blue-700">
          <div className="py-4">
            <h3 className="text-4xl font-extrabold mb-2">15+</h3>
            <p className="text-blue-200 font-medium">Years of Experience</p>
          </div>
          <div className="py-4">
            <h3 className="text-4xl font-extrabold mb-2">10k+</h3>
            <p className="text-blue-200 font-medium">Happy Patients</p>
          </div>
          <div className="py-4">
            <h3 className="text-4xl font-extrabold mb-2">50+</h3>
            <p className="text-blue-200 font-medium">Expert Therapists</p>
          </div>
          <div className="py-4">
            <h3 className="text-4xl font-extrabold mb-2">3</h3>
            <p className="text-blue-200 font-medium">Modern Clinics</p>
          </div>
        </div>
      </section>

      {/* Services Section with Professional Cards */}
      <section id="services" className="py-24 bg-slate-50">
        <div className="max-w-7xl mx-auto px-4 md:px-8">
          <div className="text-center mb-16 max-w-2xl mx-auto">
            <h2 className="text-3xl md:text-4xl font-extrabold text-blue-900 mb-4">Our Specialised Services</h2>
            <p className="text-slate-600">Comprehensive care plans designed to restore mobility, reduce pain, and improve your overall quality of life.</p>
          </div>
          
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            <div className="bg-white p-8 rounded-2xl shadow-sm hover:shadow-xl hover:shadow-blue-100 transition duration-300 border border-slate-100 group">
              <div className="w-14 h-14 bg-blue-50 text-blue-600 rounded-xl flex items-center justify-center text-2xl mb-6 group-hover:bg-blue-600 group-hover:text-white transition">🩻</div>
              <h3 className="text-xl font-bold text-blue-900 mb-3">Spine & Posture</h3>
              <p className="text-slate-600 text-sm leading-relaxed mb-6">Multidimensional approach combining therapeutic interventions to alleviate cervical and lumbar symptoms.</p>
              <Link to="/login" className="text-blue-600 font-semibold text-sm hover:text-blue-800 flex items-center">Learn more <span className="ml-1">→</span></Link>
            </div>

            <div className="bg-white p-8 rounded-2xl shadow-sm hover:shadow-xl hover:shadow-blue-100 transition duration-300 border border-slate-100 group">
              <div className="w-14 h-14 bg-blue-50 text-blue-600 rounded-xl flex items-center justify-center text-2xl mb-6 group-hover:bg-blue-600 group-hover:text-white transition">🦵</div>
              <h3 className="text-xl font-bold text-blue-900 mb-3">Knee & Joint Pain</h3>
              <p className="text-slate-600 text-sm leading-relaxed mb-6">Individualized treatment plans tailored to address osteoarthritis, sports injuries, and post-surgical rehab.</p>
              <Link to="/login" className="text-blue-600 font-semibold text-sm hover:text-blue-800 flex items-center">Learn more <span className="ml-1">→</span></Link>
            </div>

            <div className="bg-white p-8 rounded-2xl shadow-sm hover:shadow-xl hover:shadow-blue-100 transition duration-300 border border-slate-100 group">
              <div className="w-14 h-14 bg-blue-50 text-blue-600 rounded-xl flex items-center justify-center text-2xl mb-6 group-hover:bg-blue-600 group-hover:text-white transition">🏃</div>
              <h3 className="text-xl font-bold text-blue-900 mb-3">Sports Rehabilitation</h3>
              <p className="text-slate-600 text-sm leading-relaxed mb-6">Collaborative effort involving athletes to recover faster, rebuild strength, and optimize athletic performance.</p>
              <Link to="/login" className="text-blue-600 font-semibold text-sm hover:text-blue-800 flex items-center">Learn more <span className="ml-1">→</span></Link>
            </div>
            
            <div className="bg-white p-8 rounded-2xl shadow-sm hover:shadow-xl hover:shadow-blue-100 transition duration-300 border border-slate-100 group">
              <div className="w-14 h-14 bg-blue-50 text-blue-600 rounded-xl flex items-center justify-center text-2xl mb-6 group-hover:bg-blue-600 group-hover:text-white transition">⚕️</div>
              <h3 className="text-xl font-bold text-blue-900 mb-3">Neuro Physiotherapy</h3>
              <p className="text-slate-600 text-sm leading-relaxed mb-6">Specialized care for stroke, paralysis, and nerve injuries to restore movement and neuromuscular function safely.</p>
              <Link to="/login" className="text-blue-600 font-semibold text-sm hover:text-blue-800 flex items-center">Learn more <span className="ml-1">→</span></Link>
            </div>

            <div className="bg-white p-8 rounded-2xl shadow-sm hover:shadow-xl hover:shadow-blue-100 transition duration-300 border border-slate-100 group">
              <div className="w-14 h-14 bg-blue-50 text-blue-600 rounded-xl flex items-center justify-center text-2xl mb-6 group-hover:bg-blue-600 group-hover:text-white transition">🏠</div>
              <h3 className="text-xl font-bold text-blue-900 mb-3">Home Care Visits</h3>
              <p className="text-slate-600 text-sm leading-relaxed mb-6">Professional healthcare sessions delivered directly to your home for post-surgery recovery and elderly care.</p>
              <Link to="/login" className="text-blue-600 font-semibold text-sm hover:text-blue-800 flex items-center">Learn more <span className="ml-1">→</span></Link>
            </div>

            <div className="bg-blue-600 p-8 rounded-2xl shadow-md text-white flex flex-col justify-center items-center text-center">
              <h3 className="text-2xl font-bold mb-4">Need a Custom Plan?</h3>
              <p className="text-blue-100 text-sm mb-8">Consult with our lead doctors to get a diagnosis and a personalized recovery roadmap.</p>
              <Link to="/login" className="bg-white text-blue-600 font-bold py-3 px-8 rounded-lg shadow hover:bg-blue-50 transition w-full">Book Consultation</Link>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-blue-950 text-blue-200 py-12">
        <div className="max-w-7xl mx-auto px-4 md:px-8 grid grid-cols-1 md:grid-cols-4 gap-8">
          <div className="md:col-span-1">
            <div className="text-white text-2xl font-bold mb-4">BeFit Clinic</div>
            <p className="text-sm text-blue-300 mb-6">Restoring mobility, strength, and confidence through expert physiotherapy care in Pune.</p>
          </div>
          <div>
            <h4 className="text-white font-bold mb-4">Our Branches</h4>
            <ul className="space-y-2 text-sm">
              <li>Karve Road, Pune</li>
              <li>Baner Road, West Pune</li>
              <li>Paud Road Clinic</li>
            </ul>
          </div>
          <div>
            <h4 className="text-white font-bold mb-4">Quick Links</h4>
            <ul className="space-y-2 text-sm">
              <li><Link to="/login" className="hover:text-white">Patient Portal</Link></li>
              <li><Link to="/staff-dashboard" className="hover:text-white">Staff Login</Link></li>
              <li><a href="#services" className="hover:text-white">Treatments</a></li>
            </ul>
          </div>
          <div>
            <h4 className="text-white font-bold mb-4">Contact Us</h4>
            <ul className="space-y-2 text-sm">
              <li>📞 +91 20 6645 5100</li>
              <li>✉️ care@befitphysiotherapy.com</li>
              <li>🕒 Mon-Sat: 8am - 9pm</li>
            </ul>
          </div>
        </div>
        <div className="max-w-7xl mx-auto px-4 md:px-8 mt-12 pt-8 border-t border-blue-900 text-sm text-center flex flex-col md:flex-row justify-between items-center">
          <p>© 2026 BeFit Physiotherapy. All rights reserved.</p>
          <div className="flex space-x-6 mt-4 md:mt-0">
            <span className="flex items-center">🔒 HIPAA Compliant</span>
            <span className="flex items-center">🛡️ Secure Data</span>
          </div>
        </div>
      </footer>
    </div>
  );
}