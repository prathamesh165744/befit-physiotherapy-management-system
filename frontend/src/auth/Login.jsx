import React, { useState } from 'react';
import axios from '../api';
import { useNavigate, Link } from 'react-router-dom';
import loginSideImage from '../assets/login_side_image.jpg'; // Adjust extension if needed

export default function Login() {
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [error, setError] = useState(() => (new URLSearchParams(window.location.search).get('expired') ? 'Your session has expired. Please log in again.' : ''));
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const res = await axios.post('/api/auth/login', formData);
      
      localStorage.setItem('token', res.data.token);
      localStorage.setItem('role', res.data.role);
      localStorage.setItem('fullName', res.data.fullName);
      localStorage.setItem('email', formData.email);
      localStorage.setItem('branchId', res.data.branchId || 'KARVE-ROAD'); 

      if (res.data.role === 'PATIENT') {
        navigate('/dashboard');
      } else if (res.data.role === 'BRANCH_MANAGER') {
        navigate('/staff-dashboard/transfers');
      } else {
        navigate('/staff-dashboard');
      }
    } catch (err) {
      setError(err.response?.data?.message || (err.response ? 'Login failed. Please try again.' : 'Cannot reach the server. Please try again.'));
    }
  };

  return (
    // Changed to flex-col for mobile, flex-row for medium screens and up
    <div className="flex flex-col md:flex-row min-h-screen bg-gray-50">
      
      {/* Left Pane - Clinic Image & Marketing */}
      {/* Takes 35% height on mobile (h-[35vh]), auto height on desktop */}
      <div className="flex w-full md:w-1/2 h-[35vh] md:h-auto relative flex-col justify-center p-6 md:p-12 text-white overflow-hidden">
        
        {/* Background Image: No blur, sharp natural look */}
        <div 
          className="absolute inset-0 z-0 bg-cover bg-center"
          style={{ backgroundImage: `url(${loginSideImage})` }}
        ></div>
        
        {/* Very minimal dark mask just to keep text readable */}
        <div className="absolute inset-0 bg-black/40 z-0"></div>

        {/* Foreground Content */}
        <div className="relative z-10 drop-shadow-md">
          <div className="flex items-center space-x-2 text-lg md:text-xl font-bold mb-4 md:mb-16">
            <span className="text-blue-400">BeFit</span>
          </div>
          {/* Text scales down on mobile to prevent overflow */}
          <h1 className="text-2xl sm:text-3xl md:text-5xl font-bold leading-tight mb-2 md:mb-6">
            Your Journey to Better Health
          </h1>
          <p className="hidden sm:block text-sm md:text-lg text-gray-100 mb-8 max-w-md">
            Advanced physiotherapy care, personalized treatment plans and expert guidance — for a healthier, stronger you.
          </p>
          <div className="hidden lg:flex flex-wrap gap-3">
            <span className="px-4 py-2 bg-black/40 rounded-full text-sm">Expert Therapists</span>
            <span className="px-4 py-2 bg-black/40 rounded-full text-sm">Multiple Branches</span>
            <span className="px-4 py-2 bg-black/40 rounded-full text-sm">Personalized Care</span>
          </div>
        </div>
      </div>

      {/* Right Pane - Login Form */}
      {/* Takes remaining height on mobile, 50% width on desktop */}
      <div className="w-full md:w-1/2 flex flex-col justify-center flex-grow p-8 sm:p-12 md:px-24 bg-white">
        <div className="max-w-md w-full mx-auto">
          <h2 className="text-2xl md:text-3xl font-bold text-gray-900 mb-2">Welcome Back!</h2>
          <p className="text-sm md:text-base text-gray-500 mb-8">Log in to your account to continue</p>

          {error && <div className="mb-4 p-3 bg-red-100 text-red-700 rounded-md text-sm">{error}</div>}

          <form onSubmit={handleLogin} className="space-y-5 md:space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Email Address</label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                placeholder="name@example.com"
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                required
              />
            </div>

            <div>
              <div className="flex justify-between mb-1">
                <label className="block text-sm font-medium text-gray-700">Password</label>
                <a href="#" className="text-sm text-blue-600 hover:underline">Forgot password?</a>
              </div>
              <input
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                placeholder="Enter your password"
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none"
                required
              />
            </div>

            <div className="flex items-center">
              <input type="checkbox" className="h-4 w-4 text-blue-600 border-gray-300 rounded" />
              <label className="ml-2 block text-sm text-gray-700">Remember me</label>
            </div>

            <button
              type="submit"
              className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-3 rounded-lg transition-colors flex justify-center items-center"
            >
              Login &rarr;
            </button>
          </form>

          <div className="mt-8 pt-6 border-t border-gray-100">
            <div className="bg-blue-50 rounded-lg p-4 flex items-start space-x-3">
              <div className="bg-blue-100 p-2 rounded-full text-blue-600 flex-shrink-0">🛡️</div>
              <div>
                <h4 className="text-sm font-semibold text-blue-900">Your data is secure with us</h4>
                <p className="text-xs text-blue-700 mt-1">We use industry-standard encryption protocols to ensure your personal health information remains private and protected at all times.</p>
              </div>
            </div>
          </div>
          
          <p className="text-center mt-6 text-sm text-gray-600">
            Don't have an account? <Link to="/register" className="text-blue-600 hover:underline font-medium">Register here</Link>
          </p>
        </div>
      </div>
    </div>
  );
}